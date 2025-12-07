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
    <div className="fixed bottom-4 left-4 flex items-center gap-2 bg-card/60 backdrop-blur-sm px-3 py-1.5 rounded-full border border-border/50 z-50">
      <motion.div
        className={`w-2 h-2 rounded-full ${colorMap[status]}`}
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
      <span className="text-xs text-mono text-muted-foreground uppercase tracking-wider">
        {status}
      </span>
    </div>
  );
};
