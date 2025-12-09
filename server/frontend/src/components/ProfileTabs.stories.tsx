import type { Meta, StoryObj } from "@storybook/react";
import { ProfileTabs } from "../components/ProfileTabs";

const meta = {
  title: "Components/ProfileTabs",
  component: ProfileTabs,
  parameters: {
    layout: "centered",
    docs: {
      description: {
        component:
          "ProfileTabs component for switching between available profiles. Responsive design adapts to all breakpoints from mobile (375px) to desktop (1536px+).",
      },
    },
  },
  tags: ["autodocs"],
  argTypes: {
    profiles: {
      description: "Array of profile names",
      control: { type: "object" },
    },
    onProfileChange: {
      description: "Callback when profile is selected",
      action: "profile-changed",
    },
  },
} satisfies Meta<typeof ProfileTabs>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    profiles: ["Profile 1", "Profile 2", "Profile 3"],
    onProfileChange: (profile: string) => console.log("Selected:", profile),
  },
};

export const MultipleProfiles: Story = {
  args: {
    profiles: ["Gaming", "Streaming", "Editing", "Music", "Default"],
    onProfileChange: (profile: string) => console.log("Selected:", profile),
  },
};

export const SingleProfile: Story = {
  args: {
    profiles: ["Default"],
    onProfileChange: (profile: string) => console.log("Selected:", profile),
  },
};

export const MobileXS: Story = {
  args: {
    profiles: ["Profile 1", "Profile 2", "Profile 3"],
    onProfileChange: (profile: string) => console.log("Selected:", profile),
  },
  parameters: {
    viewport: {
      defaultViewport: "mobile",
    },
  },
};

export const TabletMD: Story = {
  args: {
    profiles: ["Profile 1", "Profile 2", "Profile 3"],
    onProfileChange: (profile: string) => console.log("Selected:", profile),
  },
  parameters: {
    viewport: {
      defaultViewport: "tablet",
    },
  },
};

export const DesktopXL: Story = {
  args: {
    profiles: ["Profile 1", "Profile 2", "Profile 3"],
    onProfileChange: (profile: string) => console.log("Selected:", profile),
  },
  parameters: {
    viewport: {
      defaultViewport: "desktop",
    },
  },
};

export const DarkMode: Story = {
  args: {
    profiles: ["Profile 1", "Profile 2", "Profile 3"],
    onProfileChange: (profile: string) => console.log("Selected:", profile),
  },
  decorators: [
    (Story) => (
      <div className="dark bg-slate-950 p-8 rounded-lg">
        <Story />
      </div>
    ),
  ],
};
