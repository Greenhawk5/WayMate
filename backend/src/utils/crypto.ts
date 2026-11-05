import { AuthUser } from '../types.js';

const ITERATIONS = 100000;
const HASH_ALGO = 'SHA-256';

/**
 * Derives a PBKDF2 hex hash using standard Web Crypto.
 */
async function derivePbkdf2Hex(password: string, salt: string, iterations: number): Promise<string> {
  const enc = new TextEncoder();
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    enc.encode(password),
    { name: 'PBKDF2' },
    false,
    ['deriveBits']
  );
  const derivedBits = await crypto.subtle.deriveBits(
    {
      name: 'PBKDF2',
      salt: enc.encode(salt),
      iterations,
      hash: HASH_ALGO,
    },
    keyMaterial,
    256
  );
  const bytes = new Uint8Array(derivedBits);
  return Array.from(bytes)
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

/**
 * Creates a PBKDF2 password hash formatted as:
 * pbkdf2_sha256$<iterations>$<salt>$<hex_hash>
 */
export async function hashPassword(password: string, salt?: string): Promise<string> {
  const saltStr = salt || crypto.randomUUID().replace(/-/g, '').slice(0, 16);
  const hashHex = await derivePbkdf2Hex(password, saltStr, ITERATIONS);
  return `pbkdf2_sha256$${ITERATIONS}$${saltStr}$${hashHex}`;
}

/**
 * Verifies a plaintext password against a stored PBKDF2 hash.
 * Timing-safe comparison is used to prevent timing attacks.
 */
export async function verifyPassword(password: string, storedHash: string): Promise<boolean> {
  const parts = storedHash.split('$');
  if (parts.length !== 4 || parts[0] !== 'pbkdf2_sha256') {
    return false;
  }
  const iterations = parseInt(parts[1], 10);
  const salt = parts[2];
  const expectedHex = parts[3];

  if (!iterations || !salt || !expectedHex) {
    return false;
  }

  const computedHex = await derivePbkdf2Hex(password, salt, iterations);

  if (computedHex.length !== expectedHex.length) {
    return false;
  }

  // Constant-time comparison
  let result = 0;
  for (let i = 0; i < computedHex.length; i++) {
    result |= computedHex.charCodeAt(i) ^ expectedHex.charCodeAt(i);
  }
  return result === 0;
}

/**
 * Helper to encode Base64URL string.
 */
function base64UrlEncode(str: string): string {
  const bytes = new TextEncoder().encode(str);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

/**
 * Helper to decode Base64URL string.
 */
function base64UrlDecode(str: string): string {
  let base64 = str.replace(/-/g, '+').replace(/_/g, '/');
  while (base64.length % 4) {
    base64 += '=';
  }
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return new TextDecoder().decode(bytes);
}

/**
 * Signs a JWT using HMAC-SHA256.
 */
export async function signJwt(
  user: AuthUser,
  secret: string,
  expiresInSeconds: number = 86400
): Promise<{ token: string; expiresAt: number }> {
  const now = Math.floor(Date.now() / 1000);
  const exp = now + expiresInSeconds;

  const header = { alg: 'HS256', typ: 'JWT' };
  const payload = {
    sub: user.id,
    username: user.username,
    role: user.role,
    callsign: user.callsign,
    iat: now,
    exp,
  };

  const headerPart = base64UrlEncode(JSON.stringify(header));
  const payloadPart = base64UrlEncode(JSON.stringify(payload));
  const dataToSign = `${headerPart}.${payloadPart}`;

  const enc = new TextEncoder();
  const key = await crypto.subtle.importKey(
    'raw',
    enc.encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );

  const signature = await crypto.subtle.sign('HMAC', key, enc.encode(dataToSign));
  const sigBytes = new Uint8Array(signature);
  let sigBinary = '';
  for (let i = 0; i < sigBytes.byteLength; i++) {
    sigBinary += String.fromCharCode(sigBytes[i]);
  }
  const signaturePart = btoa(sigBinary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');

  return {
    token: `${dataToSign}.${signaturePart}`,
    expiresAt: exp * 1000,
  };
}

/**
 * Verifies a JWT and extracts the user claims.
 */
export async function verifyJwt(token: string, secret: string): Promise<AuthUser | null> {
  const parts = token.split('.');
  if (parts.length !== 3) {
    return null;
  }
  const [headerPart, payloadPart, signaturePart] = parts;
  const dataToSign = `${headerPart}.${payloadPart}`;

  try {
    const header = JSON.parse(base64UrlDecode(headerPart));
    if (!header || header.alg !== 'HS256' || (header.typ && header.typ.toUpperCase() !== 'JWT')) {
      return null;
    }

    const enc = new TextEncoder();
    const key = await crypto.subtle.importKey(
      'raw',
      enc.encode(secret),
      { name: 'HMAC', hash: 'SHA-256' },
      false,
      ['verify']
    );

    // Convert base64Url signature back to bytes
    let sigBase64 = signaturePart.replace(/-/g, '+').replace(/_/g, '/');
    while (sigBase64.length % 4) {
      sigBase64 += '=';
    }
    const sigBinary = atob(sigBase64);
    const sigBytes = new Uint8Array(sigBinary.length);
    for (let i = 0; i < sigBinary.length; i++) {
      sigBytes[i] = sigBinary.charCodeAt(i);
    }

    const isValid = await crypto.subtle.verify('HMAC', key, sigBytes, enc.encode(dataToSign));
    if (!isValid) {
      return null;
    }

    const payload = JSON.parse(base64UrlDecode(payloadPart));
    const now = Math.floor(Date.now() / 1000);
    if (payload.exp && payload.exp < now) {
      return null;
    }

    return {
      id: payload.sub,
      username: payload.username,
      role: payload.role,
      callsign: payload.callsign,
    };
  } catch {
    return null;
  }
}
