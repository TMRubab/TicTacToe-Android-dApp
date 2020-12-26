pragma solidity >=0.4.22 <0.8.0;

contract TicTacToe{

	struct Player{
		address location;
		string name;
		uint256 index;
		string password;
		uint256 numWin;
		uint256 numPlayed;
	}

	mapping(uint256 => Player) private players;
	uint256 private numPlayers;

	enum state{ EMPTY, PLAYER1, PLAYER2, ENDED }

	struct Game{
		Player player1;
		Player player2;
		uint len;
		mapping(uint => mapping(uint => state)) board;
		state turn;
		uint256 index;
		state winner;
		string password;
	}

	mapping(uint256 => Game) private games;
	uint256 private numGames;
	mapping(address => Player) private resident;
	mapping(address => Game) private creation;

	//event NewPlayer(string name);
	event NewGame(uint256 gameIndex, bool free, string name);
	event PlayerJoinedGame(uint256 id, string name);
	event PlayerMadeMove(uint256 gameIndex, uint row, uint col);
	//event GameOver(uint256 gameIndex);

	function addPlayer(string memory name, string memory password) public{
		players[numPlayers].location = msg.sender;
		players[numPlayers].name = name;
		players[numPlayers].index = numPlayers;
		players[numPlayers].password = password;
		players[numPlayers].numWin = 0;
		players[numPlayers].numPlayed = 0;
		resident[msg.sender] = players[numPlayers];
		++numPlayers;
	}

	function login(string memory name, uint256 id, string memory password) public{
		require(id < numPlayers && id >= 0, "invalid id!");
		require(keccak256(abi.encodePacked((players[id].name))) == keccak256(abi.encodePacked((name))) && keccak256(abi.encodePacked((players[id].password))) == keccak256(abi.encodePacked((password))), "invalid name or password, please try again.");
		players[id].location = msg.sender;
		resident[msg.sender] = players[id];
		//emit NewPlayer(name);
	}

	function createGame(uint Len, string memory password) public{
		require(resident[msg.sender].location != address(0), "Please, log in first.");
		games[numGames].player1 = resident[msg.sender];
		games[numGames].len = Len;
		for(uint i = 0; i < Len; ++i){
			for(uint j = 0; j < Len; ++j){
				games[numGames].board[i][j] = state.EMPTY;
			}
		}
		games[numGames].index = numGames;
		games[numGames].turn = state.EMPTY;
		games[numGames].password = password;
		games[numGames].player1 = resident[msg.sender];
		creation[msg.sender] = games[numGames];
		string memory emp = "";
		bool free = keccak256(abi.encodePacked((password))) == keccak256(abi.encodePacked((emp)));
		emit NewGame(numGames, free, resident[msg.sender].name);
		++numGames;
	}
	
	function joinGame(uint256 gameInd, string memory password) public{
		require(resident[msg.sender].location != address(0), "Please, log in first.");
		require(gameInd < numGames, "No such game :(");
		require(msg.sender != games[gameInd].player1.location, "You are already in this game!");
		require(games[gameInd].player2.location == address(0), "This game is already full :(");
		require(keccak256(abi.encodePacked((games[gameInd].password))) == keccak256(abi.encodePacked((password))), "Incorrect Password!");
		++games[gameInd].player1.numPlayed;
		resident[games[gameInd].player1.location] = games[gameInd].player1;
		players[games[gameInd].player1.index] = games[gameInd].player1;
		++resident[msg.sender].numPlayed;
		games[gameInd].player2 = resident[msg.sender];
		players[games[gameInd].player2.index] = resident[msg.sender];
		games[gameInd].turn = state.PLAYER1;
		emit PlayerJoinedGame(gameInd, games[gameInd].player2.name);
	}

	function move(uint256 gameInd, uint row, uint col) public{
		require(gameInd < numGames, "No such game :(");
		Game memory currentGame = games[gameInd];
		require(msg.sender == currentGame.player1.location || msg.sender == currentGame.player2.location, "You are not in this game -_-");
		require(currentGame.turn != state.ENDED, "This game has already ended!");
		require(currentGame.turn != state.EMPTY, "Please wait for the other player to join");
		Player memory currentPlayer = ( currentGame.turn == state.PLAYER1) ? currentGame.player1 : currentGame.player2;
		require(currentPlayer.location == msg.sender, "It is not your turn!");
		require(row < currentGame.len && row >= 0 && col < currentGame.len && col >= 0, "invalid move - position does not exist");
		require(games[gameInd].board[row][col] == state.EMPTY, "invalid move - position already taken");
		games[gameInd].board[row][col] = currentGame.turn;
		games[gameInd].turn = ( currentGame.turn == state.PLAYER1) ? state.PLAYER2 : state.PLAYER1;
		emit PlayerMadeMove(gameInd, row, col);
	}

	function finishGame(uint256 gameInd, int winner) public{
		require(gameInd < numGames, "No such game :(");
		require(msg.sender == games[gameInd].player1.location || msg.sender == games[gameInd].player2.location, "You are not in this game -_-");
		if(games[gameInd].turn == state.ENDED) return;
		games[gameInd].turn = state.ENDED;
		if(winner == 0)games[gameInd].winner = state.EMPTY;
		else if(winner == 1){
			games[gameInd].winner = state.PLAYER1;
			++games[gameInd].player1.numWin;
			resident[games[gameInd].player1.location] = games[gameInd].player1;
			players[games[gameInd].player1.index] = games[gameInd].player1;
		}
		else if(winner == 2){
			games[gameInd].winner = state.PLAYER2;
			++games[gameInd].player2.numWin;
			resident[games[gameInd].player2.location] = games[gameInd].player2;
			players[games[gameInd].player2.index] = games[gameInd].player2;
		}
		//emit GameOver(gameInd);
	}

	function getNumPlayed() public view returns (uint256){
		require(resident[msg.sender].location != address(0), "Please, log in first.");
		return resident[msg.sender].numPlayed;
	}

	function getNumWin() public view returns (uint256){
		require(resident[msg.sender].location != address(0), "Please, log in first.");
		return resident[msg.sender].numWin;
	}

	function getPlayerId() public view returns (uint256){
		require(resident[msg.sender].location != address(0), "Please, log in first.");
		return resident[msg.sender].index;
	}

	function getGameId() public view returns (uint256){
		require(resident[msg.sender].location != address(0), "Please, log in first.");
		return creation[msg.sender].index;
	}


	//function getActiveGames() public returns (

}
