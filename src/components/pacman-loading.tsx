import { cn } from "@/lib/utils"

interface PacmanLoadingProps {
  className?: string
  size?: "sm" | "md" | "lg"
}

const sizeClasses = {
  sm: "scale-50",
  md: "scale-75",
  lg: "scale-100",
}

export function PacmanLoading({ className, size = "md" }: PacmanLoadingProps) {
  return (
    <div
      role="status"
      aria-label="加载中"
      className={cn("load20 flex items-center justify-center", sizeClasses[size], className)}
    >
      <div></div>
      <div></div>
      <div></div>
      <div></div>
      <div></div>
    </div>
  )
}

export { Spinner } from "./ui/spinner"
