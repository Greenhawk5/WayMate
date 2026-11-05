import { MiddlewareHandler } from 'hono';
import { Bindings, Variables } from '../types.js';
import { errorResponse } from '../utils/response.js';

interface RateLimitRecord {
  count: number;
  resetAt: number;
}

/**
 * Lightweight, zero-dependency in-memory sliding-window rate limiter for Cloudflare Workers.
 * Compatible with Cloudflare Workers Free tier (no external Redis/KV required).
 */
export class MemoryRateLimiter {
  private records = new Map<string, RateLimitRecord>();
  private readonly maxRequests: number;
  private readonly windowMs: number;

  constructor(maxRequests: number, windowMs: number) {
    this.maxRequests = maxRequests;
    this.windowMs = windowMs;
  }

  /**
   * Checks whether the given key is within the allowed request rate.
   * Increments the request count on each check.
   */
  check(key: string): { allowed: boolean; remaining: number; resetInMs: number } {
    const now = Date.now();
    this.cleanup(now);

    const record = this.records.get(key);
    if (!record || now >= record.resetAt) {
      this.records.set(key, { count: 1, resetAt: now + this.windowMs });
      return { allowed: true, remaining: this.maxRequests - 1, resetInMs: this.windowMs };
    }

    if (record.count >= this.maxRequests) {
      return { allowed: false, remaining: 0, resetInMs: record.resetAt - now };
    }

    record.count++;
    return {
      allowed: true,
      remaining: this.maxRequests - record.count,
      resetInMs: record.resetAt - now,
    };
  }

  /**
   * Explicitly registers a failure increment for brute-force protection.
   */
  recordFailure(key: string): { allowed: boolean; resetInMs: number } {
    const now = Date.now();
    this.cleanup(now);

    const record = this.records.get(key);
    if (!record || now >= record.resetAt) {
      this.records.set(key, { count: 1, resetAt: now + this.windowMs });
      return { allowed: true, resetInMs: this.windowMs };
    }

    record.count++;
    return {
      allowed: record.count <= this.maxRequests,
      resetInMs: record.resetAt - now,
    };
  }

  /**
   * Resets the record for a key (e.g. on successful login).
   */
  reset(key: string) {
    this.records.delete(key);
  }

  /**
   * Periodic garbage collection of expired buckets to prevent memory leaks.
   */
  private cleanup(now: number = Date.now()) {
    if (this.records.size > 2000) {
      for (const [key, record] of this.records.entries()) {
        if (now >= record.resetAt) {
          this.records.delete(key);
        }
      }
    }
  }

  clear() {
    this.records.clear();
  }
}

// Global limiter instances
export const loginRateLimiter = new MemoryRateLimiter(5, 60_000); // 5 attempts per 60s
export const apiGlobalLimiter = new MemoryRateLimiter(120, 60_000); // 120 reqs per 60s per IP

/**
 * Hono middleware to throttle general API flooding.
 */
export const generalRateLimitMiddleware: MiddlewareHandler<{ Bindings: Bindings; Variables: Variables }> = async (c, next) => {
  const ip = c.req.header('cf-connecting-ip') || c.req.header('x-forwarded-for') || 'local';
  const result = apiGlobalLimiter.check(ip);

  c.res.headers.set('X-RateLimit-Limit', '120');
  c.res.headers.set('X-RateLimit-Remaining', result.remaining.toString());

  if (!result.allowed) {
    c.res.headers.set('Retry-After', Math.ceil(result.resetInMs / 1000).toString());
    return errorResponse(
      c,
      'TOO_MANY_REQUESTS',
      'تعداد درخواست‌ها بیش از سقف مجاز است. لطفاً کمی صبر کرده و مجدداً تلاش نمایید',
      undefined,
      429
    );
  }

  await next();
};
