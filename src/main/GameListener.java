package main;

public interface GameListener {
	public void gameOver(boolean playerWon);

	/** TODO: Does this mean anything? */
	public void levelUp();
}
