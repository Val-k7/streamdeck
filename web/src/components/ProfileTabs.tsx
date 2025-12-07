import { motion } from "framer-motion";

interface Profile {
  id: string;
  label: string;
  color?: string;
}

interface ProfileTabsProps {
  profiles: Profile[];
  activeProfile: string;
  onProfileChange: (profileId: string) => void;
}

export const ProfileTabs = ({
  profiles,
  activeProfile,
  onProfileChange,
}: ProfileTabsProps) => {
  return (
    <div className="absolute bottom-4 left-1/2 -translate-x-1/2 z-30" style={{ maxWidth: 'calc(100% - 200px)', marginLeft: '100px' }}>
      <div className="flex items-center gap-1 bg-card/80 backdrop-blur-md rounded-full p-1 border border-border/50">
        {profiles.map((profile) => (
          <motion.button
            key={profile.id}
            onClick={() => onProfileChange(profile.id)}
            className={`
              relative px-4 py-2 rounded-full text-xs text-mono font-medium uppercase tracking-wider
              transition-colors duration-200
              ${activeProfile === profile.id
                ? "text-primary-foreground"
                : "text-muted-foreground hover:text-foreground"
              }
            `}
            whileTap={{ scale: 0.95 }}
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
