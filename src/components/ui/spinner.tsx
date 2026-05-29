import { PacmanLoading } from "../pacman-loading"

interface SpinnerProps {
  className?: string
  size?: "sm" | "md" | "lg"
}

function Spinner({ className, size = "md" }: SpinnerProps) {
  return <PacmanLoading className={className} size={size} />
}

export { Spinner }
