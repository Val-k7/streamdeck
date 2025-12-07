import type { Meta, StoryObj } from "@storybook/react";
import { SettingsOverlay } from "../components/SettingsOverlay";

const meta = {
  title: "Components/SettingsOverlay",
  component: SettingsOverlay,
  parameters: {
    layout: "fullscreen",
    docs: {
      description: {
        component:
          "Modal settings dialog with responsive design. Full-screen on mobile, centered with padding on tablet/desktop. Includes tabs for different settings sections.",
      },
    },
  },
  tags: ["autodocs"],
  argTypes: {
    isOpen: {
      control: { type: "boolean" },
      description: "Whether modal is open",
    },
    onClose: {
      description: "Callback when modal is closed",
      action: "closed",
    },
  },
} satisfies Meta<typeof SettingsOverlay>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Open: Story = {
  args: {
    isOpen: true,
    onClose: () => console.log("Settings closed"),
  },
};

export const Closed: Story = {
  args: {
    isOpen: false,
    onClose: () => console.log("Settings closed"),
  },
};

export const MobileXS: Story = {
  args: {
    isOpen: true,
    onClose: () => console.log("Settings closed"),
  },
  parameters: {
    viewport: {
      defaultViewport: "mobile",
    },
    docs: {
      description: {
        story:
          "On mobile (xs), modal is full-screen with full viewport width and scrollable content.",
      },
    },
  },
};

export const TabletMD: Story = {
  args: {
    isOpen: true,
    onClose: () => console.log("Settings closed"),
  },
  parameters: {
    viewport: {
      defaultViewport: "tablet",
    },
    docs: {
      description: {
        story:
          "On tablet (md), modal is centered with appropriate padding and tab navigation visible.",
      },
    },
  },
};

export const DesktopXL: Story = {
  args: {
    isOpen: true,
    onClose: () => console.log("Settings closed"),
  },
  parameters: {
    viewport: {
      defaultViewport: "desktop",
    },
    docs: {
      description: {
        story:
          "On desktop (xl), modal has max-width constraint and optimal spacing.",
      },
    },
  },
};

export const KeyboardNavigation: Story = {
  args: {
    isOpen: true,
    onClose: () => console.log("Settings closed"),
  },
  decorators: [
    (Story) => (
      <div>
        <div className="text-sm font-semibold text-blue-600 mb-4 p-4 bg-blue-50 rounded">
          ✓ Keyboard accessible: Tab through controls, Escape to close, Enter to
          submit
        </div>
        <Story />
      </div>
    ),
  ],
};

export const FocusTrap: Story = {
  args: {
    isOpen: true,
    onClose: () => console.log("Settings closed"),
  },
  decorators: [
    (Story) => (
      <div>
        <div className="text-sm font-semibold text-green-600 mb-4 p-4 bg-green-50 rounded">
          ✓ Focus trap active: Tab stays within modal when open
        </div>
        <Story />
      </div>
    ),
  ],
};

export const DarkMode: Story = {
  args: {
    isOpen: true,
    onClose: () => console.log("Settings closed"),
  },
  decorators: [
    (Story) => (
      <div className="dark bg-slate-950 min-h-screen">
        <Story />
      </div>
    ),
  ],
};
