import { useState } from 'react';
import './App.css';
import Login from './components/Login/Login';
import Registration from './components/Registration/Registration';

function App() {
	const [loggedIn, setLoggedIn] = useState(false);
	const [username, setUsername] = useState('');
	const [isRegistering, setIsRegistering] = useState(false);

	const handleLogin = (username, isLogged) => {
		// This function is used to signify when a user is logged in. It stores the username in the username
		// state variable and sets loggedIn to true to communicate that the user is logged in.
		// This function will be modified once the login endpoint is complete so that it sends a request
		// to the backend to make sure the user is in the database.
		fetch('http://localhost:8080/login', { //Currently broken due to CORS
			method: 'POST',
			headers: {
			  'Content-Type': 'application/json',
			},
			body: JSON.stringify({ username }),
		  })
		  .then(response => {
			console.log(response);
			if (response.ok) {
			  setUsername(username);
			  setLoggedIn(isLogged);
			} else {
			  throw new Error('Login failed');
			}
		  })
	};

	const handleRegister = (username, isLogged) => {
		// This function serves as a way of registering a new user to the application. It alerts the user
		// that they have successfully registered, sets their username to what they indicated, and
		// switches the component shown from Registration to Login.
		// This function will be modified once the register endpoint is complete so that it sends a request
		// to the backend to add the user to the database.
		alert(`Successfully registered ${username}!`);
		setUsername(username);
		setIsRegistering(false);
	};

	const handleToggleMode = () => {
		// This function is used to toggle between Login and Register components being shown. This 
		// function is called by the button at the bottom of the pre-homepage to let the user
		// switch back and forth between logging in and registering.
		setIsRegistering(!isRegistering);
	};

	return (
		<div className="container">
		<h1>Welcome to Card Engine!</h1>
		{
			loggedIn 
			?
			
			( // Once we have components for the game/lobby, they go in this area
				<p>Welcome {username}!</p>
			) 
			: 
			( //User not logged in yet, prompt with login and option to register
				<>
				<p>Doesn't seem like you're logged in!</p>
				{isRegistering ? (
					<Registration onRegister={handleRegister} />
				) : (
					<Login onLogin={handleLogin} />
				)}
				<button className="toggle-btn" onClick={handleToggleMode}>
					{
						isRegistering
						? 
						'Already have a login? Login Here'
						: 
						'New player? Register Here'
					}
				</button>
				</>
			)
		}
		</div>
	);
}

export default App;