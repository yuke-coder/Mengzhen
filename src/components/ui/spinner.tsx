import { cn } from "@/lib/utils"

interface SpinnerProps {
  className?: string
  size?: "sm" | "md" | "lg"
}

const sizeClasses = {
  sm: "h-3 w-3",
  md: "h-8 w-8",
  lg: "h-10 w-10",
}

function Spinner({ className, size = "md" }: SpinnerProps) {
  return (
    <div
      role="status"
      aria-label="加载中"
      className={cn(
        "animate-spin rounded-full border-b-2 border-t-2 border-current text-foreground",
        sizeClasses[size],
        className
      )}
    />
  )
}

export { Spinner }
