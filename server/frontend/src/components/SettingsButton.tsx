import { motion } from "framer-motion";
import { Settings } from "lucide-react";

interface SettingsButtonProps {
  onClick: () => void;
}

export const SettingsButton = ({ onClick }: SettingsButtonProps) => {
  const handleClick = () => {
    console.log("[SettingsButton] clicked");
    onClick();
  };

  return (
    <motion.button
      onClick={handleClick}
      className="fixed bottom-2 xs:bottom-3 sm:bottom-4 md:bottom-6 right-2 xs:right-3 sm:right-4 md:right-6 w-9 xs:w-10 h-9 xs:h-10 rounded-full bg-card/40 backdrop-blur-sm border border-border/50 flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-card/60 hover:border-border transition-all z-50"
      whileHover={{ scale: 1.05 }}
      whileTap={{ scale: 0.95 }}
    >
      <Settings className="w-3.5 xs:w-4 h-3.5 xs:h-4" />
    </motion.button>
  );
};
