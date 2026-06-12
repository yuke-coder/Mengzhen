/**
 * 深度检查 Supabase Storage 配置
 * 查找所有可能限制文件大小的地方
 */
import { config } from 'dotenv';
import postgres from 'postgres';
import path from 'path';

config({ path: path.join(process.cwd(), '.env.local') });

const DATABASE_URL = process.env.DATABASE_URL!;
const sql = postgres(DATABASE_URL, { max: 1 });

async function main() {
  try {
    console.log('📊 Storage schema 中的所有表:');
    const tables = await sql`
      SELECT table_name FROM information_schema.tables
      WHERE table_schema = 'storage' ORDER BY table_name
    `;
    tables.forEach(t => console.log(`   - ${t.table_name}`));

    console.log('\n🔎 检查是否有 config / settings 表:');
    try {
      const settings = await sql`SELECT * FROM storage.config LIMIT 5`;
      console.log('storage.config:', JSON.stringify(settings, null, 2));
    } catch (e) {
      console.log('   (storage.config 不存在)');
    }

    try {
      const conf = await sql`SELECT * FROM storage.configuration LIMIT 5`;
      console.log('storage.configuration:', JSON.stringify(conf, null, 2));
    } catch (e) {
      console.log('   (storage.configuration 不存在)');
    }

    console.log('\n📦 storage.buckets 详细:');
    const buckets = await sql`
      SELECT id, name, public, file_size_limit,
             pg_column_size(allowed_mime_types) as mime_size,
             allowed_mime_types
      FROM storage.buckets
    `;
    buckets.forEach(b => {
      console.log(`  - ${b.name}: file_size_limit=${b.file_size_limit} (${Math.round(Number(b.file_size_limit) / 1024 / 1024)}MB) public=${b.public}`);
    });

    console.log('\n📈 storage.objects 统计:');
    try {
      const stats = await sql`
        SELECT bucket_id, COUNT(*) as object_count,
               SUM(metadata->>'size') as total_size
        FROM storage.objects
        GROUP BY bucket_id
      `;
      stats.forEach(s => console.log(`  - ${s.bucket_id}: ${s.object_count} objects, total=${s.total_size}`));
    } catch (e) {
      console.log('   (查询 objects 表失败:', e.message, ')');
    }

    console.log('\n🔐 Storage schema 函数:');
    try {
      const funcs = await sql`
        SELECT proname, prosrc
        FROM pg_proc
        WHERE pronamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'storage')
        AND proname LIKE '%size%'
        LIMIT 10
      `;
      funcs.forEach(f => console.log(`   - ${f.proname}`));
    } catch (e) {
      console.log('   (failed)', e.message);
    }

    console.log('\n💡 测试一个小文件上传 (通过 Supabase SDK)...');
    console.log('   (请手动测试上传一个 <5MB 的文件确认管道正常)');

    process.exit(0);
  } catch (err) {
    console.error('❌ 失败:', err);
    process.exit(1);
  }
}

main();
