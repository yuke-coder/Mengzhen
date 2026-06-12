import { config } from 'dotenv';
import postgres from 'postgres';
import path from 'path';

config({ path: path.join(process.cwd(), '.env.local') });

const DATABASE_URL = process.env.DATABASE_URL!;
const sql = postgres(DATABASE_URL, { max: 1 });

async function main() {
  console.log('🔎 storage.configurations:');
  try {
    const rows = await sql`SELECT * FROM storage.configurations`;
    rows.forEach(r => console.log(JSON.stringify(r, null, 2)));
  } catch (e) {
    console.log('   错误:', e.message);
  }

  console.log('\n📋 configurations 表结构:');
  const cols = await sql`
    SELECT column_name, data_type, is_nullable
    FROM information_schema.columns
    WHERE table_schema = 'storage' AND table_name = 'configurations'
  `;
  cols.forEach(c => console.log(`   ${c.column_name} (${c.data_type}) ${c.is_nullable}`));

  console.log('\n💡 测试: 直接用 Supabase SDK 上传一个小文件验证管道...');
  console.log('   (跳过，因为需要 JS 环境)');

  console.log('\n💡 查看 storage.buckets 表的 RLS 策略:');
  try {
    const policies = await sql`
      SELECT policyname, permissive, roles, cmd, qual, with_check
      FROM pg_policies
      WHERE schemaname = 'storage' AND tablename = 'buckets'
    `;
    policies.forEach(p => console.log(`   - ${p.policyname} (${p.cmd})`));
  } catch (e) {
    console.log('   RLS 检查失败:', e.message);
  }

  console.log('\n💡 检查 Supabase storage.objects 表 RLS:');
  try {
    const objPolicies = await sql`
      SELECT policyname, permissive, roles, cmd
      FROM pg_policies
      WHERE schemaname = 'storage' AND tablename = 'objects'
    `;
    objPolicies.forEach(p => console.log(`   - ${p.policyname} (${p.cmd})`));
  } catch (e) {
    console.log('   RLS 检查失败:', e.message);
  }

  process.exit(0);
}

main();
