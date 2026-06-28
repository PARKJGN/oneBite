'use client';
import { motion } from 'framer-motion';
import { usePathname } from 'next/navigation';

/**
 * 라우트 전환 애니메이션(진입 fade + slight-up).
 * template.tsx는 layout.tsx와 달리 매 이동마다 re-mount되므로, pathname을 key로 주면
 * 화면이 바뀔 때마다 initial→animate가 다시 실행된다. (App Router는 exit 애니메이션 미지원)
 */
export default function Template({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  return (
    <motion.div
      key={pathname}
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.18, ease: 'easeOut' }}
    >
      {children}
    </motion.div>
  );
}
