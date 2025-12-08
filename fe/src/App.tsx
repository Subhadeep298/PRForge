import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Home from './pages/Home';
import JiraConnect from './components/JiraConnect';
import { AuthProvider } from './context/AuthContext';

function App() {
  return (
    <Router>
      <AuthProvider>
        <div className="min-h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-violet-900">
          <Navbar />
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/jira" element={<JiraConnect />} />
          </Routes>
        </div>
      </AuthProvider>
    </Router>
  );
}

export default App;
