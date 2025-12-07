import { Settings } from "lucide-react";
import { motion } from "framer-motion";

interface SettingsButtonProps {
  onClick: () => void;
}

export const SettingsButton = ({ onClick }: SettingsButtonProps) => {
  return (
    <motion.button
      onClick={onClick}
      className="fixed bottom-4 right-4 w-10 h-10 rounded-full bg-card/40 backdrop-blur-sm border border-border/50 flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-card/60 hover:border-border transition-all"
      whileHover={{ scale: 1.05 }}
      whileTap={{ scale: 0.95 }}
    >
      <Settings className="w-4 h-4" />
    </motion.button>
  );
};
