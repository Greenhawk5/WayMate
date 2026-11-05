import { EventEmitter } from 'node:events';

/**
 * Lightweight mock WebSocket pair for unit testing WebSocket sessions in Node.js.
 */
export class MockWebSocket extends EventEmitter {
  public readyState: number = 1; // OPEN
  public sentMessages: string[] = [];
  public peer?: MockWebSocket;

  accept() {
    // Cloudflare server-side WebSocket accept
  }

  send(data: string) {
    this.sentMessages.push(data);
    if (this.peer && this.peer.readyState === 1) {
      // Simulate asynchronous delivery
      queueMicrotask(() => {
        this.peer?.emit('message', { data });
      });
    }
  }

  close(code: number = 1000, reason: string = '') {
    this.readyState = 3; // CLOSED
    this.emit('close', { code, reason });
    if (this.peer && this.peer.readyState === 1) {
      this.peer.readyState = 3;
      this.peer.emit('close', { code, reason });
    }
  }

  addEventListener(event: string, listener: (...args: any[]) => void) {
    this.on(event, listener);
  }

  removeEventListener(event: string, listener: (...args: any[]) => void) {
    this.off(event, listener);
  }

  getLastSentJson<T = any>(): T | null {
    if (this.sentMessages.length === 0) return null;
    return JSON.parse(this.sentMessages[this.sentMessages.length - 1]);
  }

  getAllSentJson<T = any>(): T[] {
    return this.sentMessages.map((m) => JSON.parse(m));
  }
}

export function createMockWebSocketPair(): [MockWebSocket, MockWebSocket] {
  const client = new MockWebSocket();
  const server = new MockWebSocket();
  client.peer = server;
  server.peer = client;
  return [client, server];
}
