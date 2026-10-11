// Fixed vocabularies shared by validation, impact estimates and the admin UI.

// Typical weights are rough averages used only for the "estimated kg diverted" figure.
const CATEGORIES = {
  phone: { label: 'Mobile phone', weightKg: 0.2 },
  laptop: { label: 'Laptop', weightKg: 2.5 },
  tablet: { label: 'Tablet', weightKg: 0.5 },
  desktop: { label: 'Desktop / monitor', weightKg: 8 },
  tv: { label: 'Television', weightKg: 12 },
  battery: { label: 'Battery', weightKg: 0.1 },
  appliance: { label: 'Small appliance', weightKg: 3 },
  accessory: { label: 'Accessories / cables', weightKg: 0.3 },
  other: { label: 'Other', weightKg: 1 },
};

const CONDITIONS = {
  working: 'Working',
  partial: 'Partially working',
  not_working: 'Not working',
};

const TYPES = ['reuse', 'recycle'];

const STATUSES = {
  pending: 'Pending review',
  scheduled: 'Pickup scheduled',
  picked_up: 'Picked up',
  completed: 'Completed',
  cancelled: 'Cancelled',
  rejected: 'Rejected',
};

const ACTIVE_STATUSES = ['pending', 'scheduled', 'picked_up'];
const FINAL_STATUSES = ['completed', 'cancelled', 'rejected'];

module.exports = { CATEGORIES, CONDITIONS, TYPES, STATUSES, ACTIVE_STATUSES, FINAL_STATUSES };
