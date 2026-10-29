import { DatabaseSync } from 'node:sqlite';
import fs from 'node:fs';
import path from 'node:path';

export function createTestD1Database(): D1Database {
  const sqlite = new DatabaseSync(':memory:');

  // Load and apply migrations
  const migrationsDir = path.resolve(process.cwd(), 'migrations');
  const migrationFiles = fs.readdirSync(migrationsDir).sort();

  for (const file of migrationFiles) {
    if (file.endsWith('.sql')) {
      const sql = fs.readFileSync(path.join(migrationsDir, file), 'utf8');
      sqlite.exec(sql);
    }
  }

  function createPreparedStatement(query: string, boundValues: any[] = []): D1PreparedStatement {
    return {
      bind(...values: any[]): D1PreparedStatement {
        return createPreparedStatement(query, values);
      },
      async first<T = unknown>(colName?: string): Promise<T | null> {
        const stmt = sqlite.prepare(query);
        const row = stmt.get(...boundValues) as any;
        if (!row) return null;
        if (colName) return (row[colName] as T) ?? null;
        return row as T;
      },
      async all<T = unknown>(): Promise<D1Result<T>> {
        const stmt = sqlite.prepare(query);
        const rows = stmt.all(...boundValues) as T[];
        return {
          results: rows,
          success: true,
          meta: {
            duration: 1,
            changes: 0,
            rows_read: rows.length,
            rows_written: 0,
            last_row_id: 0,
            changed_db: false,
            size_after: 0,
          },
        };
      },
      async run(): Promise<D1Response> {
        const stmt = sqlite.prepare(query);
        const info = stmt.run(...boundValues);
        return {
          success: true,
          meta: {
            duration: 1,
            changes: Number(info.changes),
            last_row_id: Number(info.lastInsertRowid),
            rows_read: 0,
            rows_written: Number(info.changes),
            changed_db: Number(info.changes) > 0,
            size_after: 0,
          },
        };
      },
      raw(): Promise<any[]> {
        throw new Error('Not implemented in mock');
      },
    } as unknown as D1PreparedStatement;
  }

  return {
    prepare(query: string): D1PreparedStatement {
      return createPreparedStatement(query);
    },
    async batch(statements: D1PreparedStatement[]): Promise<D1Response[]> {
      const results: D1Response[] = [];
      sqlite.exec('BEGIN TRANSACTION');
      try {
        for (const stmt of statements) {
          const res = await stmt.run();
          results.push(res);
        }
        sqlite.exec('COMMIT');
      } catch (e) {
        sqlite.exec('ROLLBACK');
        throw e;
      }
      return results;
    },
    async exec(query: string): Promise<D1ExecResult> {
      sqlite.exec(query);
      return { count: 1, duration: 1 };
    },
    dump(): Promise<ArrayBuffer> {
      throw new Error('Not implemented');
    },
  } as unknown as D1Database;
}
