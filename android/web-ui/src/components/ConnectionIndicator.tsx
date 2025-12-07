import { motion } from "framer-motion";

type ConnectionStatus = "online" | "offline" | "connecting";

interface ConnectionIndicatorProps {
  status: ConnectionStatus;
}

export const ConnectionIndicator = ({ status }: ConnectionIndicatorProps) => {
  const colorMap = {
    online: "bg-connection-online",
    offline: "bg-connection-offline",
    connecting: "bg-connection-connecting",
  };

  return (
    <div className="fixed bottom-2 xs:bottom-3 sm:bottom-4 md:bottom-6 left-2 xs:left-3 sm:left-4 md:left-6 flex items-center gap-1.5 xs:gap-2 bg-card/60 backdrop-blur-sm px-2 xs:px-3 py-1 xs:py-1.5 rounded-full border border-border/50 z-50">
      <motion.div
        className={`w-1.5 xs:w-2 h-1.5 xs:h-2 rounded-full ${colorMap[status]}`}
        animate={
          status === "connecting"
            ? { scale: [1, 1.3, 1], opacity: [1, 0.6, 1] }
            : {}
        }
        transition={{
          duration: 1.5,
          repeat: status === "connecting" ? Infinity : 0,
          ease: "easeInOut",
        }}
      />
      <span className="text-[10px] xs:text-xs text-mono text-muted-foreground uppercase tracking-wider hidden xs:inline">
        {status}
      </span>
    </div>
  );
};
