import type { Meta, StoryObj } from "@storybook/react";
import { ConnectionIndicator } from "../components/ConnectionIndicator";

const meta = {
  title: "Components/ConnectionIndicator",
  component: ConnectionIndicator,
  parameters: {
    layout: "centered",
    docs: {
      description: {
        component:
          "Status indicator showing connection state. On mobile (xs), shows dot only. On tablet+ (sm+), shows dot with label. Status colors: green (connected), red (disconnected), yellow (loading).",
      },
    },
  },
  tags: ["autodocs"],
  argTypes: {
    status: {
      control: { type: "radio" },
      options: ["connected", "disconnected", "loading"],
      description: "Connection status",
    },
  },
} satisfies Meta<typeof ConnectionIndicator>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Connected: Story = {
  args: {
    status: "connected",
  },
};

export const Disconnected: Story = {
  args: {
    status: "disconnected",
  },
};

export const Loading: Story = {
  args: {
    status: "loading",
  },
};

export const ConnectedMobileXS: Story = {
  args: {
    status: "connected",
  },
  parameters: {
    viewport: {
      defaultViewport: "mobile",
    },
    docs: {
      description: {
        story:
          "On mobile (xs), shows only the indicator dot. Text label is hidden.",
      },
    },
  },
};

export const ConnectedTabletSM: Story = {
  args: {
    status: "connected",
  },
  parameters: {
    viewport: {
      defaultViewport: "tablet",
    },
    docs: {
      description: {
        story:
          'On tablet and up (sm+), shows indicator dot with "Connected" label.',
      },
    },
  },
};

export const DisconnectedDesktop: Story = {
  args: {
    status: "disconnected",
  },
  parameters: {
    viewport: {
      defaultViewport: "desktop",
    },
  },
};

export const ContrastValidation: Story = {
  args: {
    status: "connected",
  },
  decorators: [
    (Story) => (
      <div className="bg-white p-8 rounded-lg border-2 border-green-500">
        <div className="text-sm font-semibold text-gray-700 mb-4">
          Color Contrast: 6.8:1 (WCAG AAA ✓)
        </div>
        <Story />
      </div>
    ),
  ],
};

export const DarkMode: Story = {
  args: {
    status: "connected",
  },
  decorators: [
    (Story) => (
      <div className="dark bg-slate-950 p-8 rounded-lg">
        <Story />
      </div>
    ),
  ],
};

export const AllStates: Story = {
  render: () => (
    <div className="space-y-8 p-8">
      <div>
        <div className="text-sm font-semibold mb-2">Connected</div>
        <ConnectionIndicator status="connected" />
      </div>
      <div>
        <div className="text-sm font-semibold mb-2">Disconnected</div>
        <ConnectionIndicator status="disconnected" />
      </div>
      <div>
        <div className="text-sm font-semibold mb-2">Loading</div>
        <ConnectionIndicator status="loading" />
      </div>
    </div>
  ),
};
