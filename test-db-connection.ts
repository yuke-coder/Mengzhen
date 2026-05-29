import { config } from 'dotenv';
import { drizzle } from 'drizzle-orm/postgres-js';
import postgres from 'postgres';
import path from 'path';

// 加载 .env.local 文件
config({ path: path.join(process.cwd(), '.env.local') });

async function testConnection() {
  console.log('🧪 测试数据库连接...');
  console.log('📡 连接字符串:', process.env.DATABASE_URL?.replace(/:[^:]*@/, ':***@'));

  try {
    // 创建数据库连接
    const sql = postgres(process.env.DATABASE_URL!);
    
    // 测试查询
    const result = await sql`SELECT NOW() as current_time`;
    console.log('✅ 数据库连接成功！');
    console.log('🕐 当前数据库时间:', result[0].current_time);
    
    // 检查现有表
    const tables = await sql`
      SELECT table_name 
      FROM information_schema.tables 
      WHERE table_schema = 'public'
    `;
    console.log('📊 数据库中的表:', tables.map(t => t.table_name));
    
    await sql.end();
    console.log('✅ 测试完成！');
  } catch (error) {
    console.error('❌ 数据库连接失败:', error);
    process.exit(1);
  }
}

testConnection();
