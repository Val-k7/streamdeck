import { PadConfig } from "@/hooks/useDeckStorage";
import type { Meta, StoryObj } from "@storybook/react";
import { PadEditor } from "../components/PadEditor";

// Type helper pour onSave
const handleSave = (data: Partial<PadConfig>) => console.log("Saved:", data);

const meta = {
  title: "Components/PadEditor",
  component: PadEditor,
  parameters: {
    layout: "fullscreen",
    docs: {
      description: {
        component:
          "Pad configuration editor modal. Responsive form layout: single column on mobile, multi-column grid on tablet/desktop. All inputs accessible at all breakpoints.",
      },
    },
  },
  tags: ["autodocs"],
  argTypes: {
    padId: {
      control: { type: "text" },
      description: "ID of pad being edited",
    },
    open: {
      control: { type: "boolean" },
      description: "Whether editor is open",
    },
    onClose: {
      description: "Callback when editor is closed",
      action: "closed",
    },
    onSave: {
      description: "Callback when changes are saved",
      action: "saved",
    },
  },
} satisfies Meta<typeof PadEditor>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Open: Story = {
  args: {
    padId: "pad-1",
    open: true,
    onClose: () => console.log("Editor closed"),
    onSave: handleSave,
  },
};

export const Closed: Story = {
  args: {
    padId: "pad-1",
    open: false,
    onClose: () => console.log("Editor closed"),
    onSave: handleSave,
  },
};

export const MobileXS: Story = {
  args: {
    padId: "pad-1",
    open: true,
    onClose: () => console.log("Editor closed"),
    onSave: handleSave,
  },
  parameters: {
    viewport: {
      defaultViewport: "mobile",
    },
    docs: {
      description: {
        story:
          "On mobile (xs), form fields are stacked vertically for easy scrolling and interaction.",
      },
    },
  },
};

export const TabletMD: Story = {
  args: {
    padId: "pad-1",
    open: true,
    onClose: () => console.log("Editor closed"),
    onSave: handleSave,
  },
  parameters: {
    viewport: {
      defaultViewport: "tablet",
    },
    docs: {
      description: {
        story:
          "On tablet (md), fields begin to organize into grid layout with improved spacing.",
      },
    },
  },
};

export const DesktopXL: Story = {
  args: {
    padId: "pad-1",
    open: true,
    onClose: () => console.log("Editor closed"),
    onSave: handleSave,
  },
  parameters: {
    viewport: {
      defaultViewport: "desktop",
    },
    docs: {
      description: {
        story:
          "On desktop (xl), form uses full multi-column grid layout with optimal spacing.",
      },
    },
  },
};

export const InputFields: Story = {
  args: {
    padId: "pad-1",
    open: true,
    onClose: () => console.log("Editor closed"),
    onSave: handleSave,
  },
  decorators: [
    (Story) => (
      <div>
        <div className="text-sm font-semibold text-blue-600 mb-4 p-4 bg-blue-50 rounded">
          ✓ All inputs accessible: 16px+ text, 44px+ minimum height, clear
          labels
        </div>
        <Story />
      </div>
    ),
  ],
};

export const ButtonAccessibility: Story = {
  args: {
    padId: "pad-1",
    open: true,
    onClose: () => console.log("Editor closed"),
    onSave: handleSave,
  },
  decorators: [
    (Story) => (
      <div>
        <div className="text-sm font-semibold text-green-600 mb-4 p-4 bg-green-50 rounded">
          ✓ Button targets: 44x44px minimum, keyboard activated with Enter/Space
        </div>
        <Story />
      </div>
    ),
  ],
};

export const DarkMode: Story = {
  args: {
    padId: "pad-1",
    open: true,
    onClose: () => console.log("Editor closed"),
    onSave: handleSave,
  },
  decorators: [
    (Story) => (
      <div className="dark bg-slate-950 min-h-screen">
        <Story />
      </div>
    ),
  ],
};

export const ValidationError: Story = {
  args: {
    padId: "pad-1",
    open: true,
    onClose: () => console.log("Editor closed"),
    onSave: handleSave,
  },
  decorators: [
    (Story) => (
      <div>
        <div className="text-sm font-semibold text-red-600 mb-4 p-4 bg-red-50 rounded">
          ✓ Validation: Error messages announced to screen readers with
          role="alert"
        </div>
        <Story />
      </div>
    ),
  ],
};

export const ConfirmAction: Story = {
  args: {
    padId: "pad-1",
    open: true,
    onClose: () => console.log("Editor closed"),
    onSave: handleSave,
  },
  decorators: [
    (Story) => (
      <div>
        <div className="text-sm font-semibold text-purple-600 mb-4 p-4 bg-purple-50 rounded">
          ✓ Save/Cancel buttons: Clear labeling, keyboard accessible, visible
          focus indicators
        </div>
        <Story />
      </div>
    ),
  ],
};
