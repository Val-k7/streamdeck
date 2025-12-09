import { Loader2 } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

interface Profile {
  id: string;
  label: string;
  color?: string;
}

interface ProfileTabsProps {
  profiles: Profile[];
  activeProfile: string;
  onProfileChange: (profileId: string) => void;
  isLoading?: boolean;
}

export const ProfileTabs = ({
  profiles,
  activeProfile,
  onProfileChange,
  isLoading = false,
}: ProfileTabsProps) => {
  return (
    <div
      className="absolute bottom-2 xs:bottom-3 sm:bottom-4 md:bottom-6 left-1/2 -translate-x-1/2 z-30"
      style={{ maxWidth: "calc(100% - 32px)" }}
    >
      <div className="flex items-center gap-0.5 xs:gap-1 bg-card/80 backdrop-blur-md rounded-full p-1 xs:p-1.5 sm:p-2 border border-border/50">
        {/* Loading indicator */}
        <AnimatePresence>
          {isLoading && (
            <motion.div
              initial={{ opacity: 0, width: 0 }}
              animate={{ opacity: 1, width: "auto" }}
              exit={{ opacity: 0, width: 0 }}
              className="flex items-center justify-center px-2"
            >
              <Loader2 className="h-3 w-3 xs:h-4 xs:w-4 animate-spin text-primary" />
            </motion.div>
          )}
        </AnimatePresence>
        {profiles.map((profile) => (
          <motion.button
            key={profile.id}
            onClick={() => onProfileChange(profile.id)}
            disabled={isLoading}
            className={`
              relative px-2 xs:px-3 sm:px-4 py-1 xs:py-1.5 sm:py-2 rounded-full text-[10px] xs:text-xs sm:text-sm text-mono font-medium uppercase tracking-wider
              transition-colors duration-200 whitespace-nowrap
              ${isLoading ? "opacity-50 cursor-not-allowed" : ""}
              ${
                activeProfile === profile.id
                  ? "text-primary-foreground"
                  : "text-muted-foreground hover:text-foreground"
              }
            `}
            whileTap={isLoading ? {} : { scale: 0.95 }}
          >
            {activeProfile === profile.id && (
              <motion.div
                layoutId="activeProfile"
                className="absolute inset-0 bg-primary rounded-full"
                transition={{ type: "spring", stiffness: 400, damping: 30 }}
              />
            )}
            <span className="relative z-10">{profile.label}</span>
          </motion.button>
        ))}
      </div>
    </div>
  );
};
