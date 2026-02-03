import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import InterviewList from './pages/InterviewList';
import InterviewCreate from './pages/InterviewCreate';
import InterviewDetail from './pages/InterviewDetail';
import InterviewSession from './pages/InterviewSession';
import InterviewResult from './pages/InterviewResult';
import Home from './pages/Home';
import Login from './pages/Login';
import Signup from './pages/Signup';
import OAuthCallback from './pages/OAuthCallback';
import MyPage from './pages/MyPage';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          {/* 홈 & 인증 */}
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/oauth/callback" element={<OAuthCallback />} />
          <Route path="/mypage" element={<MyPage />} />

          {/* 면접 관련 */}
          <Route path="/dashboard" element={<InterviewList />} />
          <Route path="/create" element={<InterviewCreate />} />
          <Route path="/interviews/:id" element={<InterviewDetail />} />
          <Route path="/interviews/:id/session" element={<InterviewSession />} />
          <Route path="/interviews/:id/result" element={<InterviewResult />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
