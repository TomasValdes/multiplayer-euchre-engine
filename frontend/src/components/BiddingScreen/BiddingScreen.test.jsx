import { render, screen } from '@testing-library/react';
import { describe, it, expect} from 'vitest';
import BiddingScreen from "./BiddingScreen";


//Checks to see if frontend text is visible on screen
describe('BiddingScreen', () => {

  it('should render Bidding screen with title and screen items correctly', () => {	

	render(<BiddingScreen />);

	const screenTitle = screen.getByText(/Bidding./i);
	expect(screenTitle).toBeInTheDocument();

	const candidatesuite = screen.getByText(/The candidate suit is:/i);
	expect(candidatesuite).toBeInTheDocument();

	const pickitup = screen.getByText(/Pick it up/i);
	expect(pickitup).toBeInTheDocument();

	const PassText = screen.getByText(/Pass/i);
	expect(PassText).toBeInTheDocument();    
    
  });


});