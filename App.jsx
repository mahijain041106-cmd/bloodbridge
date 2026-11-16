
import { useEffect, useRef, useState } from "react";
import "./App.css";

const API = "http://localhost:8081/api/auth";
const REQUEST_API = "http://localhost:8081/api/requests";
const ADMIN_REQUEST_API = "http://localhost:8081/api/requests/all";
const NOTIFICATION_API = "http://localhost:8081/api/notifications";
const INVENTORY_API = "http://localhost:8081/api/inventory";
const RESERVATION_API = "http://localhost:8081/api/reservations";
const ADMIN_RESERVATION_API = "http://localhost:8081/api/reservations/all";
const DASHBOARD_API = "http://localhost:8081/api/dashboard";
const components = {
  1: "Whole Blood",
  2: "Red Blood Cells (RBC)",
  3: "Plasma",
  4: "Platelets"
};

const bloodGroups = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

const requesterNavigation = [
  { id: "submit", icon: "＋", label: "Submit Request" },
  { id: "requests", icon: "▤", label: "My Requests" },
  { id: "notifications", icon: "♧", label: "Notifications" },
  { id: "status", icon: "◷", label: "Track Status" }
];

const adminNavigation = [
  { id: "inventory", icon: "▤", label: "Blood Inventory" },
  { id: "blood-requests", icon: "▤", label: "Blood Requests" },
  { id: "reservations", icon: "▣", label: "Reservations" },
  { id: "reports", icon: "▥", label: "Reports" }
];

function App() {
  // ---------------- MAIN STATE ----------------

  const [role, setRole] = useState("");
  const [page, setPage] = useState("portal");
  const [activeSection, setActiveSection] = useState("dashboard");
  const [user, setUser] = useState(null);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [adminRequests, setAdminRequests] = useState([]);
  const [adminRequestLoading, setAdminRequestLoading] = useState(false);
  const [adminReservations, setAdminReservations] = useState([]);
  const [adminReservationLoading, setAdminReservationLoading] = useState(false);
  const [dashboardData, setDashboardData] = useState(null);
  const [dashboardLoading, setDashboardLoading] = useState(false);
  const [dashboardError, setDashboardError] = useState("");
  const [form, setForm] = useState({
    fullName: "",
    username: "",
    email: "",
    password: ""
  });

  // ---------------- REQUEST STATE ----------------

  const [requestForm, setRequestForm] = useState({
    requiredGroupCode: "A+",
    componentId: "1",
    quantity: "1",
    priorityLevel: "2",
    requestNote: ""
  });

  const [requests, setRequests] = useState([]);
  const [requestLoading, setRequestLoading] = useState(false);

  // ---------------- NOTIFICATION STATE ----------------

  const [notifications, setNotifications] = useState([]);
  const [notificationLoading, setNotificationLoading] = useState(false);
  const [notificationActionId, setNotificationActionId] = useState(null);

  // ---------------- INVENTORY STATE ----------------

  const [inventory, setInventory] = useState([]);

  const [inventoryCounts, setInventoryCounts] = useState({
    total: 0,
    available: 0,
    reserved: 0,
    expired: 0,
    inactive: 0
  });

  const [inventoryLoading, setInventoryLoading] = useState(false);
  const [inventorySaving, setInventorySaving] = useState(false);

  const [inventoryForm, setInventoryForm] = useState({
    unitCode: "",
    bloodGroupCode: "A+",
    componentId: "1",
    collectionDate: "",
    expiryDate: ""
  });

  const [editingUnitId, setEditingUnitId] = useState(null);
  const [inventoryFilter, setInventoryFilter] = useState("ALL");

  // ---------------- BROWSER HISTORY ----------------

  const historyStack = useRef([]);
  const firstHistoryEntry = useRef(true);
  const ignoreNextHistoryPush = useRef(false);

  useEffect(() => {
    function handlePopState(event) {
      const previousView = event.state?.bloodBridgeView;

      if (!previousView) return;

      historyStack.current.pop();
      ignoreNextHistoryPush.current = true;

      setPage(previousView.page);
      setRole(previousView.role);
      setActiveSection(previousView.activeSection);
      setMessage("");
    }

    window.addEventListener("popstate", handlePopState);

    return () => {
      window.removeEventListener("popstate", handlePopState);
    };
  }, []);

  useEffect(() => {
    const currentView = { page, role, activeSection };

    if (firstHistoryEntry.current) {
      firstHistoryEntry.current = false;
      historyStack.current = [currentView];

      window.history.replaceState(
        {
          ...window.history.state,
          bloodBridgeView: currentView
        },
        "",
        window.location.href
      );

      return;
    }

    if (ignoreNextHistoryPush.current) {
      ignoreNextHistoryPush.current = false;
      return;
    }

    historyStack.current.push(currentView);

    window.history.pushState(
      {
        ...window.history.state,
        bloodBridgeView: currentView
      },
      "",
      window.location.href
    );
  }, [page, role, activeSection]);

  useEffect(() => {
    function handleBackspace(event) {
      if (event.key !== "Backspace") return;

      const target = event.target;

      const isTypingField =
        target instanceof HTMLElement &&
        (target.isContentEditable ||
          ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName));

      if (isTypingField) return;

      event.preventDefault();
      goBack();
    }

    window.addEventListener("keydown", handleBackspace);

    return () => {
      window.removeEventListener("keydown", handleBackspace);
    };
  }, [page, role, activeSection]);

  // ---------------- NAVIGATION ----------------

  const navigation =
    role === "REQUESTER" ? requesterNavigation : adminNavigation;

  function goBack() {
    if (historyStack.current.length > 1) {
      window.history.back();
      return;
    }

    if (page === "dashboard" && activeSection !== "dashboard") {
      setActiveSection("dashboard");
    } else if (page === "register") {
      setPage("login");
    } else if (page === "login") {
      setPage("portal");
    } else if (page === "portal") {
      setPage("home");
    } else if (page === "dashboard") {
      setPage("home");
    }
  }

  function openSection(section) {

    setActiveSection(section);
    setMessage("");

    if (section === "requests" && role === "REQUESTER") {
      loadMyRequests();
    }

    if (section === "status" && role === "REQUESTER") {
      loadMyRequests();
    }

    if (section === "notifications" && role === "REQUESTER") {
      loadNotifications();
    }

    if (section === "inventory" && role === "INVENTORY_OPERATOR") {
      resetInventoryForm();
      setInventoryFilter("ALL");
      loadInventory();
      loadInventoryCounts();
    }

    if (section === "blood-requests" && role === "INVENTORY_OPERATOR") {
      loadAdminRequests();
    }

    if (section === "reservations" && role === "INVENTORY_OPERATOR") {
      loadAdminReservations();
    }
	if (section === "reports" &&
	    (role === "ADMIN" || role === "INVENTORY_OPERATOR")) {

	  loadDashboard();

	}
  }
  function updateField(e) {
    setForm({
      ...form,
      [e.target.name]: e.target.value
    });
  }

  function updateRequestField(e) {
    setRequestForm({
      ...requestForm,
      [e.target.name]: e.target.value
    });
  }

  function updateInventoryField(e) {
    setInventoryForm({
      ...inventoryForm,
      [e.target.name]: e.target.value
    });
  }

  // ---------------- LOGIN ----------------

  async function login(e) {
    e.preventDefault();

    if (!role) {
      setMessage("Please choose your portal first.");
      return;
    }

    setMessage("");
    setLoading(true);

    try {
      const response = await fetch(`${API}/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          username: form.username,
          password: form.password,
          role
        })
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Login failed");
      }

      localStorage.setItem("bloodbridgeToken", data.token);
      localStorage.setItem("bloodbridgeUser", JSON.stringify(data.user));

      setUser(data.user);
      setActiveSection("dashboard");
      setPage("dashboard");

      setForm({
        ...form,
        password: ""
      });

      setMessage("");

      if (role === "REQUESTER") {
        loadMyRequests(false);
      }

      if (role === "INVENTORY_OPERATOR") {
        loadInventory(false);
        loadInventoryCounts();
      }
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  // ---------------- REGISTER ----------------

  async function register(e) {
    e.preventDefault();

    if (role !== "REQUESTER") {
      setMessage("Registration is available only for requesters.");
      return;
    }

    setMessage("");
    setLoading(true);

    try {
      const response = await fetch(`${API}/register`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          fullName: form.fullName,
          username: form.username,
          email: form.email,
          password: form.password
        })
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Registration failed");
      }

      setForm({
        fullName: "",
        username: "",
        email: "",
        password: ""
      });

      setMessage(data.message || "Registration successful. Please sign in.");
      setPage("login");
      setRole("REQUESTER");
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  // ---------------- LOGOUT ----------------

  function logout() {
    localStorage.removeItem("bloodbridgeToken");
    localStorage.removeItem("bloodbridgeUser");

    setUser(null);
    setRole("");
    setPage("home");
    setActiveSection("dashboard");
    setMessage("");

    setRequests([]);
    setNotifications([]);
    setInventory([]);

    setInventoryCounts({
      total: 0,
      available: 0,
      reserved: 0,
      expired: 0,
      inactive: 0
    });

    setForm({
      fullName: "",
      username: "",
      email: "",
      password: ""
    });
  }

  // ---------------- LOAD MY REQUESTS ----------------

  async function loadMyRequests(clearMessage = true) {
    setRequestLoading(true);

    if (clearMessage) setMessage("");

    try {
      const token = localStorage.getItem("bloodbridgeToken");

      const response = await fetch(`${REQUEST_API}/my`, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`
        }
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Could not load requests");
      }

      setRequests(Array.isArray(data) ? data : []);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setRequestLoading(false);
    }
  }
  // ---------------- LOAD ALL REQUESTS FOR ADMIN ----------------

  async function loadAdminRequests(clearMessage = true) {
    setAdminRequestLoading(true);

    if (clearMessage) setMessage("");

    try {
      const token = localStorage.getItem("bloodbridgeToken");

      const response = await fetch(
        "http://localhost:8081/api/requests/all",
        {
          method: "GET",
          headers: {
            Authorization: `Bearer ${token}`
          }
        }
      );

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Could not load blood requests");
      }

      setAdminRequests(Array.isArray(data) ? data : []);

    } catch (error) {
      setMessage(error.message);
    } finally {
      setAdminRequestLoading(false);
    }
  }


  const loadAdminReservations = async () => {
    setAdminReservationLoading(true);

    try {
      const token = localStorage.getItem("bloodbridgeToken");

      const response = await fetch(ADMIN_RESERVATION_API, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Unable to load reservations");
      }

      setAdminReservations(Array.isArray(data) ? data : []);

    } catch (error) {
      setMessage(error.message);
    } finally {
      setAdminReservationLoading(false);
    }
  };
  const updateReservation = async (reservationId, action) => {
    const confirmed = window.confirm(
      `Are you sure you want to ${action.toLowerCase()} this reservation?`
    );

    if (!confirmed) return;

    try {
      const token = localStorage.getItem("bloodbridgeToken");

      const response = await fetch(RESERVATION_API, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          reservationId: Number(reservationId),
          action,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Reservation update failed");
      }

      setMessage(data.message || "Reservation updated successfully.");
      await loadAdminReservations();

    } catch (error) {
      setMessage(error.message);
    }
  };
  
  async function loadDashboard() {
    setDashboardLoading(true);
    setDashboardError("");

    try {
      const token = localStorage.getItem("bloodbridgeToken");

      const response = await fetch(DASHBOARD_API, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`
        }
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Could not load dashboard");
      }

      setDashboardData(data);

    } catch (error) {
      setDashboardError(error.message);
    } finally {
      setDashboardLoading(false);
    }
  }
  // ---------------- LOAD NOTIFICATIONS ----------------

  async function loadNotifications(clearMessage = true) {
    setNotificationLoading(true);

    if (clearMessage) setMessage("");

    try {
      const token = localStorage.getItem("bloodbridgeToken");

      const response = await fetch(NOTIFICATION_API, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`
        }
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Could not load notifications");
      }

      setNotifications(
        Array.isArray(data)
          ? data
          : Array.isArray(data.notifications)
            ? data.notifications
            : []
      );
    } catch (error) {
      setMessage(error.message);
    } finally {
      setNotificationLoading(false);
    }
  }

  // ---------------- MARK NOTIFICATION READ ----------------

  async function markNotificationAsRead(notificationId) {
    setNotificationActionId(notificationId);
    setMessage("");

    try {
      const token = localStorage.getItem("bloodbridgeToken");

      const response = await fetch(`${NOTIFICATION_API}/read`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({
          notificationId: Number(notificationId),
          isRead: true
        })
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Could not update notification");
      }

      setNotifications(previous =>
        previous.map(item =>
          Number(item.notificationId) === Number(notificationId)
            ? { ...item, isRead: true }
            : item
        )
      );
    } catch (error) {
      setMessage(error.message);
    } finally {
      setNotificationActionId(null);
    }
  }

  // ---------------- DELETE NOTIFICATION ----------------

  async function deleteNotification(notificationId) {
    if (!window.confirm("Are you sure you want to delete this notification?")) {
      return;
    }

    setNotificationActionId(notificationId);
    setMessage("");

    try {
      const token = localStorage.getItem("bloodbridgeToken");

      const response = await fetch(`${NOTIFICATION_API}/delete`, {
        method: "DELETE",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({
          notificationId: Number(notificationId)
        })
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Could not delete notification");
      }

      setNotifications(previous =>
        previous.filter(
          item => Number(item.notificationId) !== Number(notificationId)
        )
      );

      setMessage("Notification deleted successfully.");
    } catch (error) {
      setMessage(error.message);
    } finally {
      setNotificationActionId(null);
    }
  }

  // ---------------- SUBMIT BLOOD REQUEST ----------------

  async function submitBloodRequest(e) {
    e.preventDefault();

    setLoading(true);
    setMessage("");

    try {
      const token = localStorage.getItem("bloodbridgeToken");

      const response = await fetch(REQUEST_API, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({
          requiredGroupCode: requestForm.requiredGroupCode,
          componentId: Number(requestForm.componentId),
          quantity: Number(requestForm.quantity),
          priorityLevel: Number(requestForm.priorityLevel),
          requestNote: requestForm.requestNote
        })
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Request submission failed");
      }

      setMessage(
        `Blood request submitted successfully! Request ID: ${data.requestId}`
      );

      setRequestForm({
        requiredGroupCode: "A+",
        componentId: "1",
        quantity: "1",
        priorityLevel: "2",
        requestNote: ""
      });

      await loadMyRequests(false);
      setActiveSection("requests");
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  // ---------------- LOAD INVENTORY ----------------

  async function loadInventory(clearMessage = true) {
    setInventoryLoading(true);

    if (clearMessage) setMessage("");

    try {
      const token = localStorage.getItem("bloodbridgeToken");

      const response = await fetch(INVENTORY_API, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`
        }
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Could not load blood inventory");
      }

      setInventory(Array.isArray(data) ? data : []);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setInventoryLoading(false);
    }
  }

  // ---------------- INVENTORY COUNTS ----------------

  async function loadInventoryCounts() {
    try {
      const token = localStorage.getItem("bloodbridgeToken");

      const response = await fetch(`${INVENTORY_API}/count`, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`
        }
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Could not load inventory summary");
      }

      setInventoryCounts({
        total: Number(data.total || 0),
        available: Number(data.available || 0),
        reserved: Number(data.reserved || 0),
        expired: Number(data.expired || 0),
        inactive: Number(data.inactive || 0)
      });
    } catch (error) {
      setMessage(error.message);
    }
  }

  // ---------------- RESET INVENTORY FORM ----------------

  function resetInventoryForm() {
    setEditingUnitId(null);

    setInventoryForm({
      unitCode: "",
      bloodGroupCode: "A+",
      componentId: "1",
      collectionDate: "",
      expiryDate: ""
    });
  }

  // ---------------- SAVE INVENTORY UNIT ----------------

  async function saveInventoryUnit(e) {
    e.preventDefault();

    setInventorySaving(true);
    setMessage("");

    try {
      const token = localStorage.getItem("bloodbridgeToken");
      const isEditing = editingUnitId !== null;

      const payload = {
        unitCode: inventoryForm.unitCode.trim(),
        bloodGroupCode: inventoryForm.bloodGroupCode,
        componentId: Number(inventoryForm.componentId),
        collectionDate: inventoryForm.collectionDate,
        expiryDate: inventoryForm.expiryDate
      };

      if (isEditing) {
        payload.unitId = Number(editingUnitId);
      }

      const response = await fetch(INVENTORY_API, {
        method: isEditing ? "PUT" : "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify(payload)
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Could not save inventory unit");
      }

      setMessage(
        isEditing
          ? "Inventory unit updated successfully."
          : "Blood unit added successfully."
      );

      resetInventoryForm();

      await loadInventory(false);
      await loadInventoryCounts();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setInventorySaving(false);
    }
  }

  // ---------------- EDIT INVENTORY UNIT ----------------

  function editInventoryUnit(item) {
    setEditingUnitId(item.unitId);

    setInventoryForm({
      unitCode: item.unitCode || "",
      bloodGroupCode: item.bloodGroupCode || "A+",
      componentId: String(item.componentId || "1"),
      collectionDate: String(item.collectionDate || "").substring(0, 10),
      expiryDate: String(item.expiryDate || "").substring(0, 10)
    });

    window.scrollTo({
      top: 0,
      behavior: "smooth"
    });
  }

  // ---------------- DELETE INVENTORY UNIT ----------------

  async function deleteInventoryUnit(unitId) {
    if (!window.confirm("Are you sure you want to delete this blood unit?")) {
      return;
    }

    setMessage("");

    try {
      const token = localStorage.getItem("bloodbridgeToken");

      const response = await fetch(INVENTORY_API, {
        method: "DELETE",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({
          unitId: Number(unitId)
        })
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Could not delete blood unit");
      }

      setMessage(data.message || "Blood unit deleted successfully.");

      await loadInventory(false);
      await loadInventoryCounts();
    } catch (error) {
      setMessage(error.message);
    }
  }

  // ---------------- MARK EXPIRED INVENTORY ----------------

  async function markInventoryExpired() {
    if (!window.confirm("Mark all past-expiry available units as expired?")) {
      return;
    }

    setMessage("");

    try {
      const token = localStorage.getItem("bloodbridgeToken");

      const response = await fetch(`${INVENTORY_API}/expire`, {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`
        }
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Could not update expired units");
      }

      setMessage(
        `${Number(data.updatedUnits || 0)} inventory unit(s) marked as expired.`
      );

      await loadInventory(false);
      await loadInventoryCounts();
    } catch (error) {
      setMessage(error.message);
    }
  }

  // ---------------- HOME PAGE ----------------

  function renderHomePage() {
    return (
      <div className="home-page">
        <header className="home-navbar">
          <div className="home-brand">
            <span className="brand-symbol">✚</span>
            <strong>BloodBridge</strong>
          </div>

          <button
            className="button-primary"
            onClick={() => {
              setMessage("");
              setPage("portal");
            }}
          >
            Get Started →
          </button>
        </header>

        <main className="home-hero">
          <div className="home-hero-content">
            <span className="eyebrow">SMART BLOOD ALLOCATION PLATFORM</span>

            <h1>
              Connecting blood needs
              <span> with verified availability.</span>
            </h1>

            <p>
              BloodBridge coordinates blood requests, monitors inventory,
              and manages reservations through a unified platform.
            </p>

            <button
              className="button-primary home-cta"
              onClick={() => setPage("portal")}
            >
              Explore BloodBridge →
            </button>

            <div className="home-features">
              <div>
                <strong>01</strong>
                <span>Submit Requests</span>
              </div>

              <div>
                <strong>02</strong>
                <span>Verify Availability</span>
              </div>

              <div>
                <strong>03</strong>
                <span>Track Progress</span>
              </div>
            </div>
          </div>

          <div className="home-hero-visual">
            <div className="home-visual-card">
              <div className="home-blood-icon">🩸</div>
              <span className="eyebrow">BLOODBRIDGE</span>
              <h2>Every request matters.</h2>
              <p>
                Bringing request management and verified inventory
                together in one place.
              </p>

              <div className="home-visual-line" />

              <div className="home-visual-footer">
                <span>Smart allocation</span>
                <span>✚</span>
              </div>
            </div>
          </div>
        </main>

        <footer className="home-footer">
          <span>© 2026 BloodBridge</span>
          <span>College Project Prototype</span>
        </footer>
      </div>
    );
  }

  // ---------------- CHOOSE YOUR PORTAL ----------------

  function renderPortalPage() {
    return (
      <div className="portal-page">
        <header className="portal-navbar">
          <div className="home-brand">
            <span className="brand-symbol">✚</span>
            <strong>BloodBridge</strong>
          </div>

        </header>

        <main className="portal-main">
          <span className="eyebrow">WELCOME TO BLOODBRIDGE</span>

          <h1>Choose Your Portal</h1>

          <p className="portal-subtitle">
            Select the portal that matches your role to continue.
          </p>

          <div className="portal-options">
            <button
              className="portal-card"
              onClick={() => {
                setRole("REQUESTER");
                setMessage("");
                setForm({
                  ...form,
                  password: ""
                });
                setPage("login");
              }}
            >
              <span className="portal-card-icon">👤</span>
              <span className="portal-card-label">REQUESTER PORTAL</span>

              <h2>Requester</h2>

              <p>
                Submit blood requests, monitor request progress,
                and receive important notifications.
              </p>

              <span className="portal-card-action">
                Continue as Requester →
              </span>
            </button>

            <button
              className="portal-card"
              onClick={() => {
                setRole("INVENTORY_OPERATOR");
                setMessage("");
                setForm({
                  ...form,
                  password: ""
                });
                setPage("login");
              }}
            >
              <span className="portal-card-icon">🏥</span>
              <span className="portal-card-label">MANAGEMENT PORTAL</span>

              <h2>Inventory Operator</h2>

              <p>
                Manage blood inventory, review requests,
                and monitor reservations.
              </p>

              <span className="portal-card-action">
                Continue as Operator →
              </span>
            </button>
          </div>

          <div className="portal-security-note">
            <span>🔒</span>
            Secure access to your BloodBridge workspace
          </div>
        </main>

        <footer className="home-footer">
          <span>© 2026 BloodBridge</span>
          <span>Coordinating Blood Requests With Verified Availability</span>
        </footer>
      </div>
    );
  }

  // ---------------- REQUEST FORM ----------------

  function renderRequestForm() {
    return (
      <div className="content-card request-content">
        <div className="section-title-row">
          <div>
            <span className="eyebrow">REQUEST MANAGEMENT</span>
            <h2>Submit Blood Request</h2>
            <p>Provide the details of the blood component you require.</p>
          </div>

          <div className="section-icon">🩸</div>
        </div>

        <form className="request-form" onSubmit={submitBloodRequest}>
          <div className="form-section-heading">
            <span className="form-step">01</span>
            <div>
              <h3>Blood requirement</h3>
              <p>Select your required blood group and component.</p>
            </div>
          </div>

          <div className="form-grid">
            <div className="field-group">
              <label>Required Blood Group</label>
              <select
                name="requiredGroupCode"
                value={requestForm.requiredGroupCode}
                onChange={updateRequestField}
                required
              >
                {bloodGroups.map(group => (
                  <option key={group} value={group}>{group}</option>
                ))}
              </select>
            </div>

            <div className="field-group">
              <label>Blood Component</label>
              <select
                name="componentId"
                value={requestForm.componentId}
                onChange={updateRequestField}
                required
              >
                {Object.entries(components).map(([id, name]) => (
                  <option key={id} value={id}>{name}</option>
                ))}
              </select>
            </div>

            <div className="field-group">
              <label>Quantity</label>
              <input
                type="number"
                name="quantity"
                min="1"
                value={requestForm.quantity}
                onChange={updateRequestField}
                required
              />
            </div>

            <div className="field-group">
              <label>Priority Level</label>
              <select
                name="priorityLevel"
                value={requestForm.priorityLevel}
                onChange={updateRequestField}
                required
              >
                <option value="1">1 - Highest</option>
                <option value="2">2 - High</option>
                <option value="3">3 - Normal</option>
                <option value="4">4 - Low</option>
              </select>
            </div>
          </div>

          <div className="form-section-heading note-heading">
            <span className="form-step">02</span>
            <div>
              <h3>Additional information</h3>
              <p>Include any useful details about your request.</p>
            </div>
          </div>

          <div className="field-group">
            <label>Additional Note <span>(Optional)</span></label>

            <textarea
              name="requestNote"
              placeholder="Enter any additional information..."
              value={requestForm.requestNote}
              onChange={updateRequestField}
              maxLength="500"
              rows="4"
            />

            <small>{requestForm.requestNote.length}/500 characters</small>
          </div>

          <div className="form-actions">
            <button
              type="button"
              className="button-secondary"
              onClick={() => openSection("dashboard")}
            >
              Cancel
            </button>

            <button
              type="submit"
              className="button-primary"
              disabled={loading}
            >
              {loading ? "Submitting..." : "Submit Request →"}
            </button>
          </div>
        </form>
      </div>
    );
  }

  // ---------------- MY REQUESTS ----------------

  function renderMyRequests() {
    return (
      <div className="content-card">
        <div className="section-title-row">
          <div>
            <span className="eyebrow">REQUEST MANAGEMENT</span>
            <h2>My Blood Requests</h2>
            <p>View and track the requests submitted through your account.</p>
          </div>

          <button
            className="button-secondary refresh-button"
            onClick={() => loadMyRequests()}
            disabled={requestLoading}
          >
            ↻ {requestLoading ? "Loading..." : "Refresh"}
          </button>
        </div>

        <div className="request-summary">
          <div>
            <span>Total requests</span>
            <strong>{requests.length}</strong>
          </div>

          <div>
            <span>Submitted by</span>
            <strong>{user?.fullName || "Requester"}</strong>
          </div>
        </div>

        {requestLoading ? (
          <div className="empty-state">
            <span className="empty-icon">◷</span>
            <h3>Loading requests</h3>
            <p>Please wait while we retrieve your request history.</p>
          </div>
        ) : requests.length === 0 ? (
          <div className="empty-state">
            <span className="empty-icon">▤</span>
            <h3>No requests yet</h3>
            <p>Your submitted blood requests will appear here.</p>

            <button
              className="button-primary"
              onClick={() => openSection("submit")}
            >
              + Create your first request
            </button>
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="requests-table">
              <thead>
                <tr>
                  <th>Request ID</th>
                  <th>Blood Group</th>
                  <th>Component</th>
                  <th>Quantity</th>
                  <th>Priority</th>
                  <th>Status</th>
                </tr>
              </thead>

              <tbody>
                {requests.map(item => (
                  <tr key={item.requestId}>
                    <td className="request-id">
                      {item.requestCode || item.requestId}
                    </td>
                    <td><strong>{item.bloodGroup}</strong></td>
                    <td>{components[item.componentId] || item.componentId}</td>
                    <td>{item.quantity}</td>
                    <td>{item.priority}</td>
                    <td>
                      <span className={`status-pill ${String(item.requestStatus || "").toLowerCase()}`}>
                        {item.requestStatus}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    );
  }

  // ---------------- NOTIFICATIONS ----------------

  function renderNotifications() {
    const unreadCount = notifications.filter(
      item =>
        item.isRead === false ||
        item.isRead === 0 ||
        item.isRead === "false"
    ).length;

    return (
      <div className="content-card">
        <div className="section-title-row">
          <div>
            <span className="eyebrow">YOUR UPDATES</span>
            <h2>Notifications</h2>
            <p>Stay informed about your blood requests and their progress.</p>
          </div>

          <button
            className="button-secondary refresh-button"
            onClick={() => loadNotifications()}
            disabled={notificationLoading}
          >
            ↻ {notificationLoading ? "Loading..." : "Refresh"}
          </button>
        </div>

        <div className="request-summary">
          <div>
            <span>Total notifications</span>
            <strong>{notifications.length}</strong>
          </div>

          <div>
            <span>Unread notifications</span>
            <strong>{unreadCount}</strong>
          </div>
        </div>

        {notificationLoading ? (
          <div className="empty-state">
            <span className="empty-icon">◷</span>
            <h3>Loading notifications</h3>
            <p>Please wait while we retrieve your latest updates.</p>
          </div>
        ) : notifications.length === 0 ? (
          <div className="empty-state">
            <span className="empty-icon">♧</span>
            <h3>No notifications yet</h3>
            <p>Updates about your submitted blood requests will appear here.</p>
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="requests-table">
              <thead>
                <tr>
                  <th>Type</th>
                  <th>Notification</th>
                  <th>Message</th>
                  <th>Date</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>

              <tbody>
                {notifications.map(item => {
                  const isRead =
                    item.isRead === true ||
                    item.isRead === 1 ||
                    item.isRead === "true";

                  const isProcessing =
                    Number(notificationActionId) === Number(item.notificationId);

                  return (
                    <tr key={item.notificationId}>
                      <td>
                        <span className="request-id">
                          {item.notificationType || "UPDATE"}
                        </span>
                      </td>

                      <td><strong>{item.title || "BloodBridge Update"}</strong></td>
                      <td>{item.message}</td>
                      <td>{formatNotificationDate(item.createdAt)}</td>

                      <td>
                        <span className={`status-pill ${isRead ? "confirmed" : "submitted"}`}>
                          {isRead ? "READ" : "UNREAD"}
                        </span>
                      </td>

                      <td>
                        <div className="inventory-row-actions">
                          {!isRead && (
                            <button
                              className="button-secondary"
                              disabled={isProcessing}
                              onClick={() =>
                                markNotificationAsRead(item.notificationId)
                              }
                            >
                              {isProcessing ? "Updating..." : "Mark as read"}
                            </button>
                          )}

                          <button
                            className="button-secondary"
                            disabled={isProcessing}
                            onClick={() =>
                              deleteNotification(item.notificationId)
                            }
                          >
                            {isProcessing ? "Processing..." : "Delete"}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    );
  }

  function renderDashboard() {
    if (dashboardLoading) {
      return <div className="loading-message">Loading dashboard...</div>;
    }

    if (dashboardError) {
      return (
        <div className="error-message">
          {dashboardError}
          <button onClick={loadDashboard}>Retry</button>
        </div>
      );
    }

    if (!dashboardData) {
      return <div className="loading-message">No dashboard data available.</div>;
    }

    const stats = [
      { label: "Total Blood Units", value: dashboardData.totalUnits },
      { label: "Available Units", value: dashboardData.availableUnits },
      { label: "Reserved Units", value: dashboardData.reservedUnits },
      { label: "Expired Units", value: dashboardData.expiredUnits },
      { label: "Total Requests", value: dashboardData.totalRequests },
      { label: "Fulfilled Requests", value: dashboardData.fulfilledRequests },
      { label: "Unfulfilled Requests", value: dashboardData.unfulfilledRequests },
      { label: "Pending Requests", value: dashboardData.pendingRequests },
      { label: "Total Reservations", value: dashboardData.totalReservations }
    ];

    return (
      <div className="dashboard-content">
        <h2>Dashboard Overview</h2>
        <p>Monitor BloodBridge system activity and inventory.</p>

        <div className="dashboard-stats-grid">
          {stats.map((item) => (
            <div className="dashboard-stat-card" key={item.label}>
              <h3>{item.label}</h3>
              <div className="dashboard-stat-value">
                {item.value ?? 0}
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }
  // ---------------- INVENTORY ----------------

  function renderInventory() {
    const filteredInventory =
      inventoryFilter === "ALL"
        ? inventory
        : inventory.filter(
            item => String(item.status || "").toUpperCase() === inventoryFilter
          );

    const countCards = [
      { label: "Total Units", value: inventoryCounts.total, icon: "▦", color: "blue-icon" },
      { label: "Available", value: inventoryCounts.available, icon: "✓", color: "green-icon" },
      { label: "Reserved", value: inventoryCounts.reserved, icon: "▣", color: "gold-icon" },
      { label: "Expired", value: inventoryCounts.expired, icon: "◷", color: "red-icon" },
      { label: "Inactive", value: inventoryCounts.inactive, icon: "−", color: "blue-icon" }
    ];

    return (
      <div className="inventory-page">
        <div className="content-card">
          <div className="section-title-row">
            <div>
              <span className="eyebrow">INVENTORY MANAGEMENT</span>
              <h2>Blood Inventory</h2>
              <p>Manage recorded blood units and monitor their availability.</p>
            </div>

            <button
              className="button-secondary refresh-button"
              onClick={() => {
                loadInventory();
                loadInventoryCounts();
              }}
              disabled={inventoryLoading}
            >
              ↻ {inventoryLoading ? "Refreshing..." : "Refresh"}
            </button>
          </div>

          <div className="inventory-summary-grid">
            {countCards.map(card => (
              <div className="inventory-stat-card" key={card.label}>
                <span className={`inventory-stat-icon ${card.color}`}>
                  {card.icon}
                </span>

                <div>
                  <small>{card.label}</small>
                  <strong>{card.value}</strong>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="content-card inventory-form-card">
          <div className="section-title-row">
            <div>
              <span className="eyebrow">
                {editingUnitId !== null ? "UPDATE RECORD" : "NEW INVENTORY RECORD"}
              </span>

              <h2>
                {editingUnitId !== null ? "Edit Blood Unit" : "Add Blood Unit"}
              </h2>

              <p>
                {editingUnitId !== null
                  ? "Update the selected inventory record."
                  : "Register a new blood unit in the inventory."}
              </p>
            </div>

            <div className="section-icon">🩸</div>
          </div>

          <form className="request-form" onSubmit={saveInventoryUnit}>
            <div className="form-grid">
              <div className="field-group">
                <label>Unit Code</label>
                <input
                  name="unitCode"
                  placeholder="Example: UNIT-008"
                  value={inventoryForm.unitCode}
                  onChange={updateInventoryField}
                  required
                />
              </div>

              <div className="field-group">
                <label>Blood Group</label>
                <select
                  name="bloodGroupCode"
                  value={inventoryForm.bloodGroupCode}
                  onChange={updateInventoryField}
                  required
                >
                  {bloodGroups.map(group => (
                    <option key={group} value={group}>{group}</option>
                  ))}
                </select>
              </div>

              <div className="field-group">
                <label>Blood Component</label>
                <select
                  name="componentId"
                  value={inventoryForm.componentId}
                  onChange={updateInventoryField}
                  required
                >
                  {Object.entries(components).map(([id, name]) => (
                    <option key={id} value={id}>{name}</option>
                  ))}
                </select>
              </div>

              <div className="field-group">
                <label>Collection Date</label>
                <input
                  type="date"
                  name="collectionDate"
                  value={inventoryForm.collectionDate}
                  onChange={updateInventoryField}
                  required
                />
              </div>

              <div className="field-group">
                <label>Expiry Date</label>
				<input
				  type="date"
				  name="expiryDate"
				  min={
				    inventoryForm.collectionDate
				      ? new Date(
				          new Date(inventoryForm.collectionDate).getTime() +
				          86400000
				        ).toISOString().substring(0, 10)
				      : undefined
				  }
				  value={inventoryForm.expiryDate}
				  onChange={updateInventoryField}
				  required
				/>
              </div>
            </div>

            <div className="form-actions">
              {editingUnitId !== null && (
                <button
                  type="button"
                  className="button-secondary"
                  onClick={resetInventoryForm}
                >
                  Cancel Edit
                </button>
              )}

              <button
                type="submit"
                className="button-primary"
                disabled={inventorySaving}
              >
                {inventorySaving
                  ? "Saving..."
                  : editingUnitId !== null
                    ? "Save Changes →"
                    : "+ Add Blood Unit"}
              </button>
            </div>
          </form>
        </div>

        <div className="content-card">
          <div className="section-title-row">
            <div>
              <span className="eyebrow">INVENTORY RECORDS</span>
              <h2>All Blood Units</h2>
              <p>Review, edit, and manage the recorded inventory.</p>
            </div>

            <button
              className="button-secondary"
              onClick={markInventoryExpired}
            >
              Update Expired Units
            </button>
          </div>

          <div className="inventory-filter-row">
            <div className="field-group inventory-filter">
              <label>Filter by status</label>

              <select
                value={inventoryFilter}
                onChange={e => setInventoryFilter(e.target.value)}
              >
                <option value="ALL">All Units</option>
                <option value="AVAILABLE">Available</option>
                <option value="RESERVED">Reserved</option>
                <option value="EXPIRED">Expired</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </div>

            <span className="inventory-result-count">
              {filteredInventory.length} record(s)
            </span>
          </div>

          {inventoryLoading ? (
            <div className="empty-state">
              <span className="empty-icon">◷</span>
              <h3>Loading inventory</h3>
              <p>Retrieving blood unit records...</p>
            </div>
          ) : filteredInventory.length === 0 ? (
            <div className="empty-state">
              <span className="empty-icon">🩸</span>
              <h3>No inventory records found</h3>
              <p>Add a blood unit or choose another status filter.</p>
            </div>
          ) : (
            <div className="table-wrapper">
              <table className="requests-table inventory-table">
                <thead>
                  <tr>
                    <th>Unit Code</th>
                    <th>Blood Group</th>
                    <th>Component</th>
                    <th>Collection Date</th>
                    <th>Expiry Date</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>

                <tbody>
                  {filteredInventory.map(item => {
                    const status = String(item.status || "").toUpperCase();
                    const isReserved = status === "RESERVED";

                    return (
                      <tr key={item.unitId}>
                        <td className="request-id">{item.unitCode}</td>

                        <td>
                          <strong>
                            {item.bloodGroupName || item.bloodGroupCode}
                          </strong>
                        </td>

                        <td>
                          {item.componentName || components[item.componentId]}
                        </td>

                        <td>{String(item.collectionDate || "").substring(0, 10)}</td>
                        <td>{String(item.expiryDate || "").substring(0, 10)}</td>

                        <td>
                          <span className={`status-pill ${status.toLowerCase()}`}>
                            {status}
                          </span>
                        </td>

                        <td>
                          <div className="inventory-row-actions">
                            <button
                              className="button-secondary"
                              disabled={isReserved}
                              title={isReserved ? "Reserved units cannot be edited" : "Edit unit"}
                              onClick={() => editInventoryUnit(item)}
                            >
                              Edit
                            </button>

                            <button
                              className="button-secondary"
                              disabled={isReserved}
                              title={isReserved ? "Reserved units cannot be deleted" : "Delete unit"}
                              onClick={() => deleteInventoryUnit(item.unitId)}
                            >
                              Delete
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    );
  }

  // ---------------- REQUESTER DASHBOARD ----------------

  function renderRequesterDashboard() {
    return (
      <div className="dashboard-welcome">
        <div className="welcome-banner">
          <div>
            <span className="eyebrow">REQUESTER PORTAL</span>

            <h2>Welcome, {user?.fullName || "Requester"} 👋</h2>

            <p>
              Manage your blood requests, follow their progress, and receive
              important updates.
            </p>

            <button
              className="button-primary"
              onClick={() => openSection("submit")}
            >
              + Submit Blood Request
            </button>
          </div>

          <div className="welcome-illustration">🩸</div>
        </div>

        <div className="dashboard-stat-grid">
          <div className="dashboard-stat-card">
            <span className="dashboard-stat-icon blue-icon">▤</span>
            <div>
              <small>My Requests</small>
              <strong>{requests.length}</strong>
            </div>
          </div>

          <div className="dashboard-stat-card">
            <span className="dashboard-stat-icon green-icon">✓</span>
            <div>
              <small>Fulfilled</small>
              <strong>
                {requests.filter(item =>
                  String(item.requestStatus || "").toUpperCase() === "FULFILLED"
                ).length}
              </strong>
            </div>
          </div>

          <div className="dashboard-stat-card">
            <span className="dashboard-stat-icon gold-icon">◷</span>
            <div>
              <small>In Progress</small>
              <strong>
                {requests.filter(item =>
                  ["SUBMITTED", "QUEUED", "PROCESSING", "PARTIALLY_FULFILLED"]
                    .includes(String(item.requestStatus || "").toUpperCase())
                ).length}
              </strong>
            </div>
          </div>
        </div>

        <div className="dashboard-quick-grid">
          <button
            className="quick-action-card"
            onClick={() => openSection("submit")}
          >
            <span>＋</span>
            <strong>Submit Request</strong>
            <small>Create a new blood request</small>
          </button>

          <button
            className="quick-action-card"
            onClick={() => openSection("requests")}
          >
            <span>▤</span>
            <strong>My Requests</strong>
            <small>View your request history</small>
          </button>

          <button
            className="quick-action-card"
            onClick={() => openSection("status")}
          >
            <span>⌁</span>
            <strong>Track Status</strong>
            <small>Follow request progress</small>
          </button>

          <button
            className="quick-action-card"
            onClick={() => openSection("notifications")}
          >
            <span>♧</span>
            <strong>Notifications</strong>
            <small>Check your latest updates</small>
          </button>
        </div>

        <div className="content-card dashboard-recent">
          <div className="section-title-row">
            <div>
              <span className="eyebrow">RECENT ACTIVITY</span>
              <h2>Recent Requests</h2>
            </div>

            <button
              className="button-secondary"
              onClick={() => openSection("requests")}
            >
              View all →
            </button>
          </div>

          {requests.length === 0 ? (
            <div className="empty-state">
              <span className="empty-icon">▤</span>
              <h3>No recent requests</h3>
              <p>Your submitted requests will be displayed here.</p>
            </div>
          ) : (
            <div className="table-wrapper">
              <table className="requests-table">
                <thead>
                  <tr>
                    <th>Request ID</th>
                    <th>Blood Group</th>
                    <th>Component</th>
                    <th>Quantity</th>
                    <th>Status</th>
                  </tr>
                </thead>

                <tbody>
                  {requests.slice(0, 5).map(item => (
                    <tr key={item.requestId}>
                      <td className="request-id">
                        {item.requestCode || item.requestId}
                      </td>

                      <td><strong>{item.bloodGroup}</strong></td>
                      <td>{components[item.componentId] || item.componentId}</td>
                      <td>{item.quantity}</td>

                      <td>
                        <span className={`status-pill ${String(item.requestStatus || "").toLowerCase()}`}>
                          {item.requestStatus}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    );
  }

  // ---------------- INVENTORY OPERATOR DASHBOARD ----------------

  function renderAdminDashboard() {
    return (
      <div className="dashboard-welcome">
        <div className="welcome-banner admin-welcome-banner">
          <div>
            <span className="eyebrow">INVENTORY OPERATOR PORTAL</span>

            <h2>Welcome, {user?.fullName || "Operator"} 👋</h2>

            <p>
              Manage blood inventory, monitor requests and review reservations.
            </p>
          </div>

          <div className="welcome-illustration">🏥</div>
        </div>

        <div className="management-section">
          <div className="management-heading">
            <h2>Management tools</h2>
            <p>Choose a section to manage your BloodBridge system.</p>
          </div>

          <div className="management-grid">
            <button
              className="management-card"
              onClick={() => openSection("inventory")}
            >
              <span className="management-icon red-management-icon">✚</span>

              <span className="management-card-content">
                <strong>Blood Inventory</strong>
                <small>View and manage blood units</small>
              </span>

              <span className="management-arrow">→</span>
            </button>

            <button
              className="management-card"
              onClick={() => openSection("blood-requests")}
            >
              <span className="management-icon blue-management-icon">▤</span>

              <span className="management-card-content">
                <strong>Blood Requests</strong>
                <small>Review submitted requests</small>
              </span>

              <span className="management-arrow">→</span>
            </button>

            <button
              className="management-card"
              onClick={() => openSection("reservations")}
            >
              <span className="management-icon purple-management-icon">▣</span>

              <span className="management-card-content">
                <strong>Reservations</strong>
                <small>Manage reserved blood units</small>
              </span>

              <span className="management-arrow">→</span>
            </button>

            <button
              className="management-card"
              onClick={() => openSection("reports")}
            >
              <span className="management-icon green-management-icon">▥</span>

              <span className="management-card-content">
                <strong>Reports</strong>
                <small>View system reports and analytics</small>
              </span>

              <span className="management-arrow">→</span>
            </button>
          </div>
        </div>
      </div>
    );
  }

  // ---------------- TRACK REQUEST STATUS ----------------

  function renderTrackStatus() {
    const stages = [
      {
        id: "SUBMITTED",
        title: "Request Submitted",
        description: "Your request has been received."
      },
      {
        id: "QUEUED",
        title: "In Processing Queue",
        description: "Your request is waiting for processing."
      },
      {
        id: "PROCESSING",
        title: "Processing",
        description: "The system is checking compatible inventory."
      },
      {
        id: "PARTIALLY_FULFILLED",
        title: "Partially Fulfilled",
        description: "Some requested units have been allocated."
      },
      {
        id: "FULFILLED",
        title: "Fulfilled",
        description: "The request has been fully allocated."
      }
    ];

    return (
      <div className="content-card">
        <div className="section-title-row">
          <div>
            <span className="eyebrow">REQUEST TRACKING</span>
            <h2>Track Request Status</h2>
            <p>Follow the latest status of your submitted blood requests.</p>
          </div>

          <button
            className="button-secondary"
            onClick={() => loadMyRequests()}
            disabled={requestLoading}
          >
            ↻ Refresh
          </button>
        </div>

        {requestLoading ? (
          <div className="empty-state">
            <h3>Loading request status...</h3>
          </div>
        ) : requests.length === 0 ? (
          <div className="empty-state">
            <span className="empty-icon">⌁</span>
            <h3>No requests to track</h3>
            <p>Submit a blood request to see its progress here.</p>

            <button
              className="button-primary"
              onClick={() => openSection("submit")}
            >
              Submit Blood Request
            </button>
          </div>
        ) : (
          <div className="tracking-list">
            {requests.map(item => {
              const status = String(
                item.requestStatus || "SUBMITTED"
              ).toUpperCase();

              const stageIndex = stages.findIndex(
                stage => stage.id === status
              );

              const isFailed = [
                "CANCELLED",
                "UNFULFILLED",
                "FAILED"
              ].includes(status);

              return (
                <div className="tracking-card" key={item.requestId}>
                  <div className="tracking-card-header">
                    <div>
                      <span className="eyebrow">
                        {item.requestCode || `REQUEST #${item.requestId}`}
                      </span>

                      <h3>
                        {item.bloodGroup} · {components[item.componentId] || "Blood Component"}
                      </h3>

                      <p>Quantity: {item.quantity}</p>
                    </div>

                    <span className={`status-pill ${status.toLowerCase()}`}>
                      {status.replaceAll("_", " ")}
                    </span>
                  </div>

                  {isFailed ? (
                    <div className="tracking-terminal-status">
                      <strong>
                        Request status: {status.replaceAll("_", " ")}
                      </strong>

                      <p>
                        This request is no longer moving through the normal
                        allocation process. Check with the inventory operator
                        for further information.
                      </p>
                    </div>
                  ) : (
                    <div className="tracking-timeline">
                      {stages.map((stage, index) => {
                        const isComplete =
                          stageIndex >= 0 && index <= stageIndex;

                        const isCurrent = index === stageIndex;

                        return (
                          <div
                            className={`tracking-stage ${isComplete ? "completed" : ""} ${isCurrent ? "current" : ""}`}
                            key={stage.id}
                          >
                            <div className="tracking-stage-marker">
                              {isComplete ? "✓" : index + 1}
                            </div>

                            <div className="tracking-stage-content">
                              <strong>{stage.title}</strong>
                              <p>{stage.description}</p>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    );
  }
  // ---------------- ADMIN BLOOD REQUESTS ----------------

  function renderAdminRequests() {
    const total = adminRequests.length;

    const pending = adminRequests.filter(item =>
      ["SUBMITTED", "QUEUED", "PROCESSING"].includes(
        String(item.requestStatus || "").toUpperCase()
      )
    ).length;

    const fulfilled = adminRequests.filter(item =>
      String(item.requestStatus || "").toUpperCase() === "FULFILLED"
    ).length;

    return (
      <div className="content-card">
        <div className="section-title-row">
          <div>
            <span className="eyebrow">REQUEST MANAGEMENT</span>
            <h2>All Blood Requests</h2>
            <p>Review blood requests submitted by registered requesters.</p>
          </div>

          <button
            className="button-secondary refresh-button"
            onClick={() => loadAdminRequests()}
            disabled={adminRequestLoading}
          >
            ↻ {adminRequestLoading ? "Loading..." : "Refresh"}
          </button>
        </div>

        <div className="request-summary">
          <div>
            <span>Total Requests</span>
            <strong>{total}</strong>
          </div>

          <div>
            <span>Pending / Processing</span>
            <strong>{pending}</strong>
          </div>

          <div>
            <span>Fulfilled</span>
            <strong>{fulfilled}</strong>
          </div>
        </div>

        {adminRequestLoading ? (
          <div className="empty-state">
            <span className="empty-icon">◷</span>
            <h3>Loading blood requests</h3>
            <p>Please wait while we retrieve all submitted requests.</p>
          </div>
        ) : adminRequests.length === 0 ? (
          <div className="empty-state">
            <span className="empty-icon">▤</span>
            <h3>No blood requests found</h3>
            <p>Submitted requester records will appear here.</p>
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="requests-table">
              <thead>
                <tr>
                  <th>Request ID</th>
                  <th>Requester</th>
                  <th>Blood Group</th>
                  <th>Component</th>
                  <th>Quantity</th>
                  <th>Priority</th>
                  <th>Validation</th>
                  <th>Request Status</th>
                  <th>Submitted At</th>
                </tr>
              </thead>

              <tbody>
                {adminRequests.map(item => {
                  const status = String(
                    item.requestStatus || ""
                  ).toUpperCase();

                  return (
                    <tr key={item.requestId}>
                      <td className="request-id">
                        {item.requestCode || item.requestId}
                      </td>

                      <td>
                        <strong>
                          {item.requesterName || `User ${item.requesterUserId}`}
                        </strong>
                      </td>

                      <td>
                        <strong>{item.bloodGroup}</strong>
                      </td>

                      <td>
                        {components[item.componentId] || item.componentId}
                      </td>

                      <td>{item.quantity}</td>

                      <td>{item.priority}</td>

                      <td>
                        <span className="status-pill">
                          {item.validationStatus || "N/A"}
                        </span>
                      </td>

                      <td>
                        <span className={`status-pill ${status.toLowerCase()}`}>
                          {status.replaceAll("_", " ")}
                        </span>
                      </td>

                      <td>
                        {formatNotificationDate(item.submittedAt)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    );
  }
  function renderAdminReservations() {
    return (
      <div className="content-card">
        <div className="section-title-row">
          <div>
            <span className="eyebrow">MANAGEMENT</span>
            <h2>Reservation Management</h2>
            <p>View and manage blood unit reservations.</p>
          </div>

          <button
            className="refresh-button"
            onClick={loadAdminReservations}
          >
            Refresh
          </button>
        </div>

        {adminReservationLoading ? (
          <p>Loading reservations...</p>
        ) : adminReservations.length === 0 ? (
          <div className="empty-state">
            No reservations found.
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="requests-table">
              <thead>
                <tr>
                  <th>Reservation</th>
                  <th>Requester</th>
                  <th>Blood Group</th>
                  <th>Blood Unit</th>
                  <th>Status</th>
                  <th>Reserved At</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {adminReservations.map((item) => (
                  <tr key={item.reservationId}>
                    <td>{item.reservationCode}</td>
                    <td>{item.requesterName}</td>
                    <td>{item.bloodGroup}</td>
                    <td>{item.unitCode}</td>
                    <td>
                      <span className="status-pill">
                        {item.status}
                      </span>
                    </td>
                    <td>
                      {formatNotificationDate(item.reservedAt)}
                    </td>
                    <td>
                      {item.status === "RESERVED" && (
                        <div className="inventory-row-actions">
                          <button
                            onClick={() =>
                              updateReservation(item.reservationId, "CONFIRM")
                            }
                          >
                            Confirm
                          </button>

                          <button
                            onClick={() =>
                              updateReservation(item.reservationId, "CANCEL")
                            }
                          >
                            Cancel
                          </button>
                        </div>
                      )}

                      {item.status !== "RESERVED" && "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    );
  }
  // ---------------- PLACEHOLDER SECTIONS ----------------

  function renderPlaceholderSection() {
    const details = {
      "blood-requests": {
        icon: "▤",
        title: "Blood Requests",
        description: "The request management view is not connected to this dashboard yet."
      },

      reservations: {
        icon: "▣",
        title: "Reservation Management",
        description: "The reservation management view is not connected to this dashboard yet."
      },

      reports: {
        icon: "▥",
        title: "Reports & Analytics",
        description: "Reporting and analytics will be available here."
      }
    };

    const current = details[activeSection] || {
      icon: "◈",
      title: getSectionTitle(),
      description: "This section is not available yet."
    };

    return (
      <div className="content-card">
        <div className="empty-state">
          <span className="empty-icon">{current.icon}</span>
          <h2>{current.title}</h2>
          <p>{current.description}</p>

          <button
            className="button-primary"
            onClick={() => openSection("dashboard")}
          >
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }

  // ---------------- SECTION TITLE ----------------

  function getSectionTitle() {
    const titles = {
      dashboard: "Dashboard",
      submit: "Submit Blood Request",
      requests: role === "REQUESTER" ? "My Requests" : "Blood Requests",
      "blood-requests": "Blood Requests",
      notifications: "Notifications",
      status: "Track Request Status",
      inventory: "Blood Inventory",
      reservations: "Reservations",
      reports: "Reports"
    };

    return titles[activeSection] || "Dashboard";
  }

  // ---------------- DATE FORMAT ----------------

  function formatNotificationDate(dateValue) {
    if (!dateValue) return "Date unavailable";

    const date = new Date(dateValue);

    if (Number.isNaN(date.getTime())) {
      return String(dateValue).replace("T", " ");
    }

    return date.toLocaleString();
  }

  // ---------------- LOGIN AND REGISTER SCREEN ----------------


  function renderAuthScreen() {
    const isRegister = page === "register";

    return (
      <div className="auth-page">
        <div className="auth-brand-panel">
          <div className="auth-brand">
            <span className="brand-symbol">✚</span>
            <span>BloodBridge</span>
          </div>

          <div className="auth-brand-content">
            <span className="eyebrow">SMART BLOOD ALLOCATION</span>
            <h1>
              Connecting blood needs with verified availability.
            </h1>
            <p>
              A coordinated platform for managing blood requests, inventory,
              and reservations.
            </p>
          </div>

          <div className="auth-brand-footer">
            <span>. BloodBridge ·</span>
          </div>
        </div>

        <div className="auth-form-panel">
          <div className="auth-form-container">
            <button
              className="auth-back-button"
              onClick={goBack}
              type="button"
            >
              ← Back
            </button>

            <span className="eyebrow">
              {isRegister ? "CREATE YOUR ACCOUNT" : "WELCOME TO BLOODBRIDGE"}
            </span>

            <h2>
              {isRegister ? "Create an account" : "Sign in to continue"}
            </h2>

            <p className="auth-subtitle">
              {isRegister
                ? "Enter your details to create a requester account."
                : role === "REQUESTER"
                  ? "Sign in to manage and track your blood requests."
                  : "Sign in to access the inventory management workspace."}
            </p>

            <div className="selected-portal-indicator">
              <span>{role === "REQUESTER" ? "👤" : "📦"}</span>

              <div>
                <small>SELECTED PORTAL</small>
                <strong>
                  {role === "REQUESTER" ? "Requester" : "Inventory Operator"}
                </strong>
              </div>

              {!isRegister && (
                <button
                  type="button"
                  className="auth-change-button"
                  onClick={() => {
                    setMessage("");
                    setPage("portal");
                  }}
                >
                  Change Portal
                </button>
              )}
            </div>

            <form
              className="auth-form"
              onSubmit={isRegister ? register : login}
            >
              {isRegister && (
                <div className="field-group">
                  <label>Full Name</label>
                  <input
                    type="text"
                    name="fullName"
                    placeholder="Enter your full name"
                    value={form.fullName}
                    onChange={updateField}
                    required
                  />
                </div>
              )}

              <div className="field-group">
                <label>Username</label>
                <input
                  type="text"
                  name="username"
                  placeholder="Enter your username"
                  value={form.username}
                  onChange={updateField}
                  required
                />
              </div>

              {isRegister && (
                <div className="field-group">
                  <label>Email Address</label>
                  <input
                    type="email"
                    name="email"
                    placeholder="Enter your email"
                    value={form.email}
                    onChange={updateField}
                    required
                  />
                </div>
              )}

              <div className="field-group">
                <label>Password</label>
                <input
                  type="password"
                  name="password"
                  placeholder="Enter your password"
                  value={form.password}
                  onChange={updateField}
                  required
                />
              </div>

              {message && (
                <div className="auth-message">
                  {message}
                </div>
              )}

              <button
                type="submit"
                className="button-primary auth-submit-button"
                disabled={loading}
              >
                {loading
                  ? "Please wait..."
                  : isRegister
                    ? "Create Account →"
                    : "Sign In →"}
              </button>
            </form>

            {role === "REQUESTER" && (
              <div className="auth-switch">
                {isRegister ? (
                  <>
                    Already have an account?{" "}
                    <button
                      type="button"
                      onClick={() => {
                        setMessage("");
                        setPage("login");
                      }}
                    >
                      Sign in
                    </button>
                  </>
                ) : (
                  <>
                    New to BloodBridge?{" "}
                    <button
                      type="button"
                      onClick={() => {
                        setMessage("");
                        setForm({
                          ...form,
                          password: ""
                        });
                        setPage("register");
                      }}
                    >
                      Create an account
                    </button>
                  </>
                )}
              </div>
            )}

            <p className="auth-disclaimer">
              BloodBridge is an educational project prototype and is not a
              clinical blood distribution service.
            </p>
          </div>
        </div>
      </div>
    );
  }


  // ---------------- MAIN APPLICATION ----------------

  if (page === "home") {
    return renderHomePage();
  }

  if (page === "portal") {
    return renderPortalPage();
  }

  if (page !== "dashboard" || !user) {
    return renderAuthScreen();
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span className="brand-symbol">✚</span>

          <div>
            <strong>BloodBridge</strong>
            <small>SMART ALLOCATION</small>
          </div>
        </div>

        <div className="sidebar-profile">
          <div className="profile-avatar">
            {(user.fullName || "U").charAt(0).toUpperCase()}
          </div>

          <div className="profile-details">
            <strong>{user.fullName || "User"}</strong>
            <small>
              {role === "REQUESTER" ? "Requester" : "Inventory Operator"}
            </small>
          </div>
        </div>

        <div className="sidebar-label">WORKSPACE</div>

        <nav className="sidebar-navigation">
          <button
            className={`nav-item ${activeSection === "dashboard" ? "active" : ""}`}
            onClick={() => openSection("dashboard")}
          >
            <span className="nav-icon">⌂</span>
            <span>Dashboard</span>
          </button>

          {navigation.map(item => (
            <button
              key={item.id}
              className={`nav-item ${activeSection === item.id ? "active" : ""}`}
              onClick={() => openSection(item.id)}
            >
              <span className="nav-icon">{item.icon}</span>
              <span>{item.label}</span>
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="sidebar-project-note">
            <span>✦</span>

            <div>
              <strong>BloodBridge</strong>
              <small>Coordinating verified availability</small>
            </div>
          </div>

          <button className="logout-button" onClick={logout}>
            <span>↪</span>
            Sign Out
          </button>
        </div>
      </aside>

      <main className="main-panel">
        <header className="topbar">
          <div className="topbar-left">
            <button
              className="topbar-back"
              onClick={goBack}
              title="Go back"
            >
              ←
            </button>

            <div>
              <span className="topbar-eyebrow">BLOODBRIDGE WORKSPACE</span>
              <h1>{getSectionTitle()}</h1>
            </div>
          </div>

          <div className="topbar-right">
            <div className="topbar-user">
              <span className="topbar-user-avatar">
                {(user.fullName || "U").charAt(0).toUpperCase()}
              </span>

              <div>
                <strong>{user.fullName || "User"}</strong>
                <small>
                  {role === "REQUESTER" ? "Requester" : "Inventory Operator"}
                </small>
              </div>
            </div>
          </div>
        </header>

        {message && (
          <div className="global-message">
            <span>{message}</span>

            <button
              onClick={() => setMessage("")}
              aria-label="Dismiss message"
            >
              ×
            </button>
          </div>
        )}

        <section className="page-content">
          {activeSection === "dashboard" &&
            role === "REQUESTER" &&
            renderRequesterDashboard()}

          {activeSection === "dashboard" &&
            role === "INVENTORY_OPERATOR" &&
            renderAdminDashboard()}

          {activeSection === "submit" &&
            role === "REQUESTER" &&
            renderRequestForm()}

          {activeSection === "requests" &&
            role === "REQUESTER" &&
            renderMyRequests()}

          {activeSection === "status" &&
            role === "REQUESTER" &&
            renderTrackStatus()}

          {activeSection === "notifications" &&
            role === "REQUESTER" &&
            renderNotifications()}

		  {activeSection === "reports" &&
			(role === "ADMIN" || role === "INVENTORY_OPERATOR") &&
		    renderDashboard()}
			  
          {activeSection === "inventory" &&
            role === "INVENTORY_OPERATOR" &&
            renderInventory()}
			
		  {activeSection === "blood-requests" &&
			  role === "INVENTORY_OPERATOR" &&
			  renderAdminRequests()}
		  
			  {activeSection === "reservations" &&
			    role === "INVENTORY_OPERATOR" &&
			    renderAdminReservations()}

			  {activeSection !== "dashboard" &&
			    activeSection !== "submit" &&
			    activeSection !== "requests" &&
			    activeSection !== "status" &&
			    activeSection !== "notifications" &&
			    activeSection !== "inventory" &&
			    activeSection !== "blood-requests" &&
				activeSection !== "reservations"&&
			    renderPlaceholderSection()}
        </section>

        <footer className="app-footer">
          <span>© 2026 BloodBridge</span>
          <span>
            Coordinating Blood Requests With Verified Availability
          </span>
        </footer>
      </main>
    </div>
  );
}

export default App;
