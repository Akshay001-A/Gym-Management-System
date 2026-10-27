import { useState } from "react";
import { Bell, CalendarDays, ChevronDown, Menu, UserRound } from "lucide-react";

export default function Topbar({ page, setMobileNav, setNotice }) {
  const [showProfile, setShowProfile] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);

  const today = new Date().toLocaleDateString("en-GB", {
    day: "2-digit",
    month: "long",
    year: "numeric",
  });

  return (
    <header className="topbar">
      <div className="topbar-left">
        <button
          type="button"
          className="mobile-menu"
          aria-label="Toggle navigation"
          onClick={() => setMobileNav((current) => !current)}
        >
          <Menu size={21} />
        </button>

        <div className="breadcrumbs">
          Workspace <span>/</span> {page}
        </div>
      </div>

      <div className="topbar-actions">
        <div className="today-label">
          <CalendarDays size={17} />
          <span>{today}</span>
        </div>

        <div className="topbar-menu-wrap">
          <button
            type="button"
            className="icon-button"
            aria-label="Notifications"
            aria-expanded={showNotifications}
            onClick={() => {
              setShowNotifications((current) => !current);
              setShowProfile(false);
            }}
          >
            <Bell size={18} />
          </button>

          {showNotifications && (
            <div className="topbar-popover">
              <strong>Notifications</strong>
              <p>You’re all caught up.</p>
              <button
                type="button"
                className="popover-action"
                onClick={() => {
                  setShowNotifications(false);
                  setNotice("No new notifications.");
                }}
              >
                Mark as read
              </button>
            </div>
          )}
        </div>

        <div className="topbar-menu-wrap">
          <button
            type="button"
            className="profile-trigger"
            aria-label="Administrator profile"
            aria-expanded={showProfile}
            onClick={() => {
              setShowProfile((current) => !current);
              setShowNotifications(false);
            }}
          >
            <span className="top-account">A</span>
            <ChevronDown size={15} />
          </button>

          {showProfile && (
            <div className="topbar-popover profile-popover">
              <div className="profile-summary">
                <span className="profile-summary-icon">
                  <UserRound size={18} />
                </span>
                <div>
                  <strong>Administrator</strong>
                  <p>Club manager</p>
                </div>
              </div>
              <button
                type="button"
                className="popover-action"
                onClick={() => {
                  setShowProfile(false);
                  setNotice("Profile settings are not connected yet.");
                }}
              >
                Profile settings
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}