import type { Meta, StoryObj } from "@storybook/react";
import { SettingsButton } from "../components/SettingsButton";

const meta = {
  title: "Components/SettingsButton",
  component: SettingsButton,
  parameters: {
    layout: "centered",
    docs: {
      description: {
        component:
          "Floating action button for opening settings. Responsive sizing: 36px on mobile, 40px on tablet/desktop. Always maintains 44px+ touch target with padding.",
      },
    },
  },
  tags: ["autodocs"],
  argTypes: {
    onClick: {
      description: "Callback when button is clicked",
      action: "clicked",
    },
  },
} satisfies Meta<typeof SettingsButton>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    onClick: () => console.log("Settings clicked"),
  },
};

export const Hovered: Story = {
  args: {
    onClick: () => console.log("Settings clicked"),
  },
  decorators: [
    (Story) => (
      <div className="hover:shadow-lg transition-shadow">
        <Story />
      </div>
    ),
  ],
};

export const MobileXS: Story = {
  args: {
    onClick: () => console.log("Settings clicked"),
  },
  parameters: {
    viewport: {
      defaultViewport: "mobile",
    },
  },
};

export const TabletMD: Story = {
  args: {
    onClick: () => console.log("Settings clicked"),
  },
  parameters: {
    viewport: {
      defaultViewport: "tablet",
    },
  },
};

export const DesktopXL: Story = {
  args: {
    onClick: () => console.log("Settings clicked"),
  },
  parameters: {
    viewport: {
      defaultViewport: "desktop",
    },
  },
};

export const DarkMode: Story = {
  args: {
    onClick: () => console.log("Settings clicked"),
  },
  decorators: [
    (Story) => (
      <div className="dark bg-slate-950 p-8 rounded-lg">
        <Story />
      </div>
    ),
  ],
};

export const TouchTarget: Story = {
  args: {
    onClick: () => console.log("Settings clicked"),
  },
  decorators: [
    (Story) => (
      <div className="bg-white p-8 rounded-lg border-2 border-blue-500">
        <div className="text-sm font-semibold text-blue-600 mb-4">
          Touch Target (44px minimum)
        </div>
        <Story />
      </div>
    ),
  ],
};
