import type { Preview } from "@storybook/react";
import "../src/index.css";
import "../src/styles/globals.css";

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
    viewport: {
      viewports: {
        mobile: {
          name: "Mobile (375px)",
          styles: {
            width: "375px",
            height: "667px",
          },
          type: "mobile",
        },
        tablet: {
          name: "Tablet (768px)",
          styles: {
            width: "768px",
            height: "1024px",
          },
          type: "tablet",
        },
        desktop: {
          name: "Desktop (1440px)",
          styles: {
            width: "1440px",
            height: "900px",
          },
          type: "desktop",
        },
        wide: {
          name: "Wide (1920px)",
          styles: {
            width: "1920px",
            height: "1080px",
          },
          type: "desktop",
        },
      },
    },
  },
  decorators: [
    (Story) => (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 dark:from-slate-950 dark:to-slate-900 p-4">
        <Story />
      </div>
    ),
  ],
};

export default preview;
