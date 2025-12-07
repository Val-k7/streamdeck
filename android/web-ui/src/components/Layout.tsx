/**
 * Layout - Composant conteneur principal réutilisable
 * Gère responsivité, padding, et structure de base des pages
 */

import { cn } from "@/lib/utils";
import { ReactNode } from "react";

interface LayoutProps {
  children: ReactNode;
  className?: string;
  padded?: boolean;
  fullScreen?: boolean;
  center?: boolean;
  direction?: "row" | "column";
  gap?: "xs" | "sm" | "md" | "lg" | "xl";
}

const gapClasses = {
  xs: "gap-1",
  sm: "gap-2",
  md: "gap-4",
  lg: "gap-6",
  xl: "gap-8",
};

/**
 * Layout - Conteneur principal pour les pages
 * Fournit une structure responsif cohérente
 *
 * @example
 * <Layout padded center direction="column">
 *   <DeckGrid />
 * </Layout>
 */
export const Layout = ({
  children,
  className,
  padded = true,
  fullScreen = false,
  center = false,
  direction = "column",
  gap = "md",
}: LayoutProps) => {
  return (
    <div
      className={cn(
        // Base
        "w-full",
        fullScreen && "h-screen",
        fullScreen && "overflow-hidden",

        // Flex
        "flex",
        direction === "row" ? "flex-row" : "flex-col",
        gapClasses[gap],

        // Center
        center && "items-center justify-center",

        // Padding responsif
        padded &&
          "px-2 py-2 xs:px-4 xs:py-3 sm:px-4 sm:py-4 md:px-6 md:py-6 lg:px-8 lg:py-8",

        // Classes personnalisées
        className
      )}
    >
      {children}
    </div>
  );
};

interface PageLayoutProps {
  children: ReactNode;
  className?: string;
  header?: ReactNode;
  footer?: ReactNode;
}

/**
 * PageLayout - Layout complet pour une page avec header et footer optionnels
 */
export const PageLayout = ({
  children,
  className,
  header,
  footer,
}: PageLayoutProps) => {
  return (
    <Layout
      direction="column"
      fullScreen
      padded={false}
      className={cn("", className)}
    >
      {header && (
        <header className="border-b border-border bg-card px-4 py-4 xs:px-4 xs:py-4 sm:px-6 sm:py-6 md:px-8 md:py-8">
          {header}
        </header>
      )}

      <main className="flex-1 overflow-auto px-2 py-2 xs:px-4 xs:py-3 sm:px-4 sm:py-4 md:px-6 md:py-6 lg:px-8 lg:py-8">
        {children}
      </main>

      {footer && (
        <footer className="border-t border-border bg-card px-4 py-4 xs:px-4 xs:py-4 sm:px-6 sm:py-6 md:px-8 md:py-8">
          {footer}
        </footer>
      )}
    </Layout>
  );
};

interface ContainerProps {
  children: ReactNode;
  className?: string;
  size?: "sm" | "md" | "lg" | "xl" | "2xl" | "full";
  center?: boolean;
}

/**
 * Container - Conteneur avec max-width responsif
 */
export const Container = ({
  children,
  className,
  size = "lg",
  center = true,
}: ContainerProps) => {
  const sizeClasses = {
    sm: "max-w-sm",
    md: "max-w-md",
    lg: "max-w-lg",
    xl: "max-w-xl",
    "2xl": "max-w-2xl",
    full: "w-full",
  };

  return (
    <div
      className={cn(
        "w-full",
        sizeClasses[size],
        center && "mx-auto",
        className
      )}
    >
      {children}
    </div>
  );
};

interface GridLayoutProps {
  children: ReactNode;
  className?: string;
  columns?: "2" | "3" | "4" | "6" | "auto";
  gap?: "xs" | "sm" | "md" | "lg" | "xl";
}

/**
 * GridLayout - Grille responsif pour composants
 */
export const GridLayout = ({
  children,
  className,
  columns = "auto",
  gap = "md",
}: GridLayoutProps) => {
  const columnClasses = {
    "2": "grid-cols-2",
    "3": "grid-cols-3 md:grid-cols-3",
    "4": "grid-cols-2 sm:grid-cols-3 md:grid-cols-4",
    "6": "grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6",
    auto: "grid-cols-auto",
  };

  return (
    <div
      className={cn("grid", columnClasses[columns], gapClasses[gap], className)}
    >
      {children}
    </div>
  );
};

export default Layout;
