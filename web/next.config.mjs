/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone', // Docker 슬림 이미지용(필요 파일만 .next/standalone 으로 묶음)
};
export default nextConfig;
