# Cloud Native Scanner - Node.js WebUI

A modern, responsive web interface built with Next.js, React, and TypeScript for the Cloud Native Topic Scanner.

## 🚀 Features

### Modern UI/UX
- **Responsive Design**: Mobile-first approach with Tailwind CSS
- **Dark Mode Support**: Automatic theme switching
- **Real-time Updates**: Live data refresh with SWR
- **Interactive Charts**: Data visualization with Recharts
- **Smooth Animations**: Framer Motion for enhanced UX

### Dashboard
- **Overview Statistics**: Key metrics and KPIs
- **Recent Activity Feed**: Real-time updates
- **Top Themes Chart**: Visual theme distribution
- **Quick Actions**: Fast access to common tasks

### Topic Management
- **Advanced Filtering**: Filter by theme, source, date, score
- **Search Functionality**: Full-text search across topics
- **Sorting Options**: Multiple sort criteria
- **Pagination**: Efficient data loading
- **Export Options**: CSV, JSON, Excel formats

### Theme Management
- **Theme Browser**: Visual theme exploration
- **Topic Grouping**: Topics organized by themes
- **Theme Statistics**: Usage metrics and trends

### Admin Panel
- **Source Management**: Configure data sources
- **Search Topic Configuration**: Manage search queries
- **System Monitoring**: Health status and metrics
- **Prompt Management**: LLM prompt configuration

### Real-time Features
- **Live Updates**: WebSocket connections for real-time data
- **Notifications**: Toast notifications for user feedback
- **Progress Indicators**: Loading states and progress bars

## 🛠️ Technology Stack

### Frontend
- **Next.js 14**: React framework with App Router
- **React 18**: Modern React with hooks
- **TypeScript**: Type-safe development
- **Tailwind CSS**: Utility-first CSS framework
- **Headless UI**: Accessible UI components
- **Heroicons**: Beautiful SVG icons

### State Management
- **SWR**: Data fetching and caching
- **React Query**: Server state management
- **React Hook Form**: Form handling
- **Zod**: Schema validation

### Data Visualization
- **Recharts**: Chart library
- **React Table**: Advanced table functionality
- **React Virtualized**: Performance optimization

### Development Tools
- **ESLint**: Code linting
- **Prettier**: Code formatting
- **Jest**: Testing framework
- **Testing Library**: Component testing

## 📦 Installation

### Prerequisites
- Node.js 18+ 
- npm 8+
- Docker (optional)

### Local Development

1. **Clone and Install**
```bash
cd webui-nodejs
npm install
```

2. **Environment Setup**
```bash
cp .env.example .env.local
# Edit .env.local with your configuration
```

3. **Start Development Server**
```bash
npm run dev
```

4. **Open Browser**
```
http://localhost:3000
```

### Docker Deployment

1. **Build Image**
```bash
npm run docker:build
```

2. **Run Container**
```bash
npm run docker:run
```

## 🔧 Configuration

### Environment Variables

```bash
# API Configuration
NEXT_PUBLIC_API_URL=http://localhost:8082
BACKEND_URL=http://localhost:8082

# Feature Flags
NEXT_PUBLIC_ENABLE_REAL_TIME=true
NEXT_PUBLIC_ENABLE_DARK_MODE=true
NEXT_PUBLIC_ENABLE_ANALYTICS=false

# UI Configuration
NEXT_PUBLIC_DEFAULT_PAGE_SIZE=20
NEXT_PUBLIC_MAX_PAGE_SIZE=100
NEXT_PUBLIC_REFRESH_INTERVAL=30000
```

### Tailwind Configuration

Customize the design system in `tailwind.config.js`:

```javascript
module.exports = {
  theme: {
    extend: {
      colors: {
        primary: {
          // Custom primary colors
        }
      }
    }
  }
}
```

## 📁 Project Structure

```
webui-nodejs/
├── components/           # Reusable UI components
│   ├── Layout.tsx       # Main layout component
│   ├── ui/              # Basic UI components
│   ├── dashboard/       # Dashboard-specific components
│   ├── forms/           # Form components
│   └── charts/          # Chart components
├── hooks/               # Custom React hooks
│   └── useApi.ts        # API integration hooks
├── lib/                  # Utility libraries
│   └── api.ts           # API client
├── pages/                # Next.js pages
│   ├── _app.tsx         # App wrapper
│   ├── index.tsx        # Dashboard
│   ├── themes/          # Theme pages
│   ├── topics/          # Topic pages
│   └── admin/           # Admin pages
├── styles/               # Global styles
│   └── globals.css      # Tailwind CSS
├── types/                # TypeScript definitions
│   └── index.ts         # Type definitions
├── public/               # Static assets
├── Dockerfile           # Container configuration
├── next.config.js       # Next.js configuration
├── tailwind.config.js   # Tailwind configuration
└── package.json         # Dependencies
```

## 🎨 Component Library

### Layout Components
- `Layout`: Main application layout with sidebar
- `Header`: Top navigation bar
- `Sidebar`: Navigation sidebar
- `Footer`: Page footer

### UI Components
- `Button`: Styled button variants
- `Card`: Content containers
- `Modal`: Dialog overlays
- `Table`: Data tables with sorting/filtering
- `Form`: Form components with validation
- `LoadingSpinner`: Loading indicators
- `Toast`: Notification system

### Dashboard Components
- `StatsCard`: Metric display cards
- `Chart`: Data visualization
- `ActivityFeed`: Recent activity list
- `QuickActions`: Action shortcuts

### Data Components
- `TopicList`: Topic display lists
- `ThemeBrowser`: Theme exploration
- `SearchResults`: Search result display
- `FilterPanel`: Advanced filtering

## 🔌 API Integration

### API Client
The `apiClient` provides a centralized way to interact with the backend:

```typescript
import apiClient from '../lib/api'

// Get topics
const topics = await apiClient.getTopics(filters, sort, page, size)

// Create theme
const theme = await apiClient.createTheme(themeData)

// Export data
const blob = await apiClient.exportTopics('csv', filters)
```

### Custom Hooks
Use custom hooks for data fetching:

```typescript
import { useTopics, useThemes, useDashboardStats } from '../hooks/useApi'

function TopicsPage() {
  const { data: topics, isLoading, error } = useTopics()
  const { data: themes } = useThemes()
  const { data: stats } = useDashboardStats()
  
  // Component logic
}
```

## 🧪 Testing

### Unit Tests
```bash
npm test
```

### Test Coverage
```bash
npm run test:coverage
```

### E2E Tests
```bash
npm run test:e2e
```

## 🚀 Deployment

### Production Build
```bash
npm run build
npm start
```

### Docker Deployment
```bash
docker build -t cloud-native-scanner-webui .
docker run -p 8080:8080 cloud-native-scanner-webui
```

### Kubernetes Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: webui
spec:
  replicas: 3
  selector:
    matchLabels:
      app: webui
  template:
    metadata:
      labels:
        app: webui
    spec:
      containers:
      - name: webui
        image: hrexed/cloudnatviescaner-webui:0.1
        ports:
        - containerPort: 8080
        env:
        - name: NEXT_PUBLIC_API_URL
          value: "http://topic-analyzer:8082"
```

## 🔄 Migration from Java WebUI

### Key Differences

| Feature | Java WebUI | Node.js WebUI |
|---------|------------|---------------|
| **Framework** | Spring Boot + Thymeleaf | Next.js + React |
| **Styling** | Bootstrap | Tailwind CSS |
| **State Management** | Server-side | Client-side with SWR |
| **Real-time** | Polling | WebSockets |
| **Performance** | Server-rendered | Client-side + SSR |
| **Development** | Maven | npm |
| **Deployment** | JAR file | Docker container |

### Migration Benefits

1. **Better Performance**: Client-side rendering with caching
2. **Enhanced UX**: Real-time updates and smooth animations
3. **Modern Development**: Hot reload, TypeScript, modern tooling
4. **Scalability**: Stateless frontend, easy horizontal scaling
5. **Maintainability**: Component-based architecture
6. **Mobile Support**: Responsive design with mobile-first approach

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

MIT License - see LICENSE file for details.
