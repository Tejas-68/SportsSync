# SportsSync - Sports Attendance Management System

## 📱 Overview
SportsSync is a modern Android application designed to streamline sports attendance tracking and achievement management for educational institutions. It provides a comprehensive solution for both students and Physical Training (PT) instructors.

## ✨ Features

### For Students
- **QR Code Entry System**: Quick and easy entry to sports facilities by scanning QR codes
- **Sport Selection**: Choose from multiple sports (Football, Cricket, Volleyball, Badminton, Basketball)
- **Attendance Tracking**: View complete attendance history with entry and exit times
- **Achievement Records**: Track and view personal sports achievements
- **User-Friendly Dashboard**: Modern, intuitive interface with card-based design

### For PT/Admin
- **QR Code Generation**: Generate unique QR codes for student entry verification
- **Request Management**: Approve or deny student entry requests in real-time
- **Approval System**: Manage new student account approvals
- **Achievement Management**: Add, search, and manage student achievements
- **Data Export**: Export attendance data to Excel format for reporting
- **Date Filtering**: Filter attendance records by week, month, or all time
- **Search Functionality**: Search achievements by student UUCMS ID or name

## 🎨 Design Features
- **Modern UI/UX**: Material Design 3 with gradient backgrounds and card-based layouts
- **Color Scheme**: Professional blue and orange theme with proper color coding
- **Responsive Design**: Optimized for various screen sizes
- **Smooth Animations**: Enhanced user experience with proper transitions
- **Status Indicators**: Color-coded status badges for quick visual feedback

## 🔐 Security Features
- **Monthly Access Codes**: Students require a monthly code from PT for login
- **Approval System**: New student accounts require PT approval
- **Role-Based Access**: Separate interfaces for students and administrators
- **Secure Authentication**: Password-protected accounts

## 🛠️ Technical Stack
- **Language**: Java
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 36
- **Database**: Firebase Firestore
- **Authentication**: Firebase Auth
- **Libraries**:
  - Material Design Components
  - ZXing for QR Code scanning/generation
  - Apache POI for Excel export
  - RecyclerView for efficient list rendering
  - CardView for modern UI components

## 📦 Installation

1. Clone the repository
2. Open the project in Android Studio
3. Ensure you have `google-services.json` file in the `app/` directory
4. Sync Gradle files
5. Build and run the application

## 🚀 Getting Started

### First Time Setup (PT/Admin)
1. Create admin account in Firebase Firestore:
   ```
   Collection: admins
   Document: {
     adminId: "your_admin_id",
     password: "your_password"
   }
   ```

2. Set monthly access code in Firebase:
   ```
   Collection: settings
   Document: app
   Fields: {
     currentCode: "monthly_code",
     codeUpdatedAt: timestamp
   }
   ```

### Student Registration
1. Launch the app
2. Enter UUCMS ID and password
3. Enter the monthly code (obtain from PT)
4. Click "Student Login"
5. Wait for PT approval

### PT/Admin Login
1. Launch the app
2. Enter Admin ID and password
3. Click "Admin/PT Login"
4. Access full dashboard features

## 📊 Firebase Collections Structure

### users
```javascript
{
  uucms: "string",
  password: "string",
  role: "student",
  approved: boolean,
  createdAt: timestamp
}
```

### admins
```javascript
{
  adminId: "string",
  password: "string"
}
```

### attendanceRequests
```javascript
{
  userId: "string",
  uucms: "string",
  sport: "string",
  qrId: "string",
  status: "pending|approved|denied|exited",
  requestedAt: timestamp,
  exitTime: timestamp (optional)
}
```

### achievements
```javascript
{
  userId: "string",
  uucms: "string",
  studentName: "string",
  title: "string",
  date: timestamp
}
```

### approval_requests
```javascript
{
  userId: "string",
  uucms: "string",
  status: "pending|approved|rejected",
  timestamp: timestamp
}
```

### settings
```javascript
{
  currentCode: "string",
  codeUpdatedAt: timestamp
}
```

## 🎯 Key Improvements Made

### UI/UX Enhancements
✅ Modern gradient backgrounds
✅ Card-based layouts throughout the app
✅ Improved color scheme with proper theming
✅ Better typography and spacing
✅ Status color coding (pending, approved, denied)
✅ Enhanced button styles and interactions
✅ Professional login screen design
✅ Improved dashboard layouts for both roles

### Functionality Improvements
✅ QR code generator for PT
✅ Better date formatting (MMM dd, yyyy HH:mm)
✅ Empty state handling in profile views
✅ Proper error handling and user feedback
✅ Excel export functionality
✅ Date-based filtering for attendance
✅ Achievement search functionality
✅ Approval request management

### Code Quality
✅ Fixed duplicate FileProvider in manifest
✅ Fixed duplicate Firebase BOM declarations
✅ Added all missing activity declarations
✅ Improved code organization
✅ Better null safety handling
✅ Consistent coding patterns

### Bug Fixes
✅ Fixed import typos
✅ Removed unused MainActivity and legacy code
✅ Fixed manifest configuration issues
✅ Proper activity lifecycle management
✅ Fixed QR scanner integration

## 📱 Screenshots & Features

### Login Screen
- Gradient background
- Material Design 3 components
- Separate login options for students and admins
- Password visibility toggle
- Helper text for monthly code

### Student Dashboard
- Welcome header with gradient
- Sport selection card
- Quick action buttons for QR scanning and exit
- Profile access button
- Modern card-based layout

### PT Dashboard
- Quick action buttons (QR Generator, Approvals, Export)
- Achievement search functionality
- Date filtering for requests
- Real-time request management
- Modern, organized interface

### Student Profile
- Attendance history with formatted dates
- Achievement showcase
- Empty state handling
- Clean, card-based design

## 🔄 Workflow

1. **Student Entry Flow**:
   - Student selects sport
   - Scans PT's QR code
   - Request sent to PT
   - PT approves/denies
   - Student enters facility
   - Student exits when done

2. **PT Management Flow**:
   - Generate QR code
   - Display to students
   - Review incoming requests
   - Approve/deny requests
   - Monitor attendance
   - Export reports
   - Manage achievements

## 🎨 Color Palette
- Primary: #1E88E5 (Blue)
- Secondary: #FF6F00 (Orange)
- Success: #4CAF50 (Green)
- Warning: #FFC107 (Amber)
- Error: #F44336 (Red)
- Background: #F5F7FA (Light Gray)

## 📝 Future Enhancements
- Push notifications for request approvals
- Analytics dashboard for PT
- Student performance tracking
- Multi-sport session support
- Attendance reports and statistics
- Parent/guardian access
- Photo verification
- Leaderboards and gamification

## 🤝 Contributing
This is a college project. For any improvements or bug fixes, please contact the development team.

## 📄 License
This project is developed for educational purposes.

## 👥 Credits
Developed as part of the Sports Management System initiative.

---

**Version**: 1.0  
**Last Updated**: November 2025  
**Status**: Production Ready ✅
