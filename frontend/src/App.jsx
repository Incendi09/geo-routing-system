import { Routes, Route } from 'react-router-dom';
import Home from './routes/Home';
import Header from './components/Header';
import './App.css';

/**
 * Main App component.
 * Sets up routing and the overall layout.
 */
function App() {
    return (
        <div className="app">
            <Header />
            <main className="main">
                <Routes>
                    <Route path="/" element={<Home />} />
                </Routes>
            </main>
        </div>
    );
}

export default App;
