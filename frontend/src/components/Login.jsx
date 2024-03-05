import React from "react";
import { useState } from "react";
import './Login.css'

function Login({ onLogin }) {
    const [username, setUsername] = useState('');
  
    const handleLogin = () => {
      // Perform your login logic here (e.g., API call, authentication)
      // For simplicity, let's just check if username is not empty
      if (username) {
        onLogin(username, true);
      } else {
        alert('Please enter username');
      }
    };
  
    return (
      <div className="login-box">
        <h2>Login</h2>
        <input
          type="text"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />
        <button className='login-btn' onClick={handleLogin}>Login</button>
      </div>
    );
  }
  
  export default Login;