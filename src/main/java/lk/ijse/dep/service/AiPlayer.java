package lk.ijse.dep.service;

import java.util.*;

public class AiPlayer extends Player {

    public AiPlayer(Board board) {
        super(board);

    }

    @Override
    public void movePiece(int col) {

        ///////////// MATH.RANDOM /////////////
    /*    do {
            col = (int) (Math.random()*6);
        }while (!(col > -1 && col < 6) || !(board.isLegalMove(col)));

*/

        /////////////>>>> MCTS ALGORITHM <<</////////////

        MctsAlgorithm mcts = new MctsAlgorithm(board.getBoardImpl());
        col = mcts.doMcts(); // Determine the next move using MCTS

        if (this.board.isLegalMove(col)) {

            // If the move is legal, update the game board and notify the UI
            this.board.updateMove(col, Piece.GREEN);
            this.board.getBoardUI().update(col, false);

            if (this.board.findWinner().getWinningPiece() == Piece.EMPTY) {
                // If no winner yet and no legal moves left, notify the winner
                if (!this.board.exitsLegalMoves()) {
                    this.board.getBoardUI().notifyWinner(this.board.findWinner());
                }
            } else {
                // If there is a winner, notify the UI
                this.board.getBoardUI().notifyWinner(this.board.findWinner());
            }
        }
    }

    static class MctsAlgorithm {

        static class Node {
            //incoder to catch BoardImpl is board(2d ayyay)
            BoardImpl board;
            int value;
            int visit;
            Node parent;
            List<Node> children = new ArrayList<>();
            // node save this array... automatically grow array

            public Node(BoardImpl board) {

                this.board = board;
            }

            // Find the child node with the maximum value
            Node getMaxValueChild() {
                Node result = children.get(0);

                for (int i = 1; i < children.size(); i++) {
                    if (children.get(i).value > result.value) {
                        result = children.get(i);
                    }
                }
                return result;
            }
            // Add a child node to the list of children
            void addChild(Node child) {
                children.add(child);
            }
        }

        BoardImpl board;
        ///int playerId;
       // int oppositePlayerId;

        public MctsAlgorithm(BoardImpl board) {
            // initialize BoardImpl is board
            this.board = board;
        }

        public int doMcts() {
            System.out.println("MCTS ALGORITHM working :).");
            int count = 0;

            //creat new node
            Node tree = new Node(board);

            while (count < 4000) {
                count++;

                // Select Node

                Node promisingNode = select(tree);

                // Expand Node
                Node selected = promisingNode;

                if (selected.board.getStatus()) {
                    selected = expand(promisingNode);
                }

                // Simulate
                Piece resultPiece = simulate(selected);

                // Propagate
                backPropagation(resultPiece, selected);
            }

            Node best = tree.getMaxValueChild();

            System.out.println("Best move score " + best.value + " & times visits " + best.visit);

            return best.board.col;
        }

        private Node select(Node tree) {
            Node node = tree;
            while (node.children.size() != 0) {
                node = findBestNodeWithUCT(node);
            }
            return node;
        }

        Node expand(Node node) {
            BoardImpl board = node.board;

            for (BoardImpl move : getAllLegalMoves(board)) {
                Node child = new Node(move);
                child.parent = node;
                node.addChild(child);
            }

            Random rand = new Random();

            int random = rand.nextInt(node.children.size());

            return node.children.get(random);
        }

        private Piece simulate(Node promisingNode) {
            // Simulate the game and return the result piece (winning piece)
            Node node = new Node(promisingNode.board);
            node.parent = promisingNode.parent;
            Winner winner = node.board.findWinner();

            if (winner.getWinningPiece() == Piece.BLUE) {
                node.parent.value = Integer.MIN_VALUE;
                return node.board.findWinner().getWinningPiece();
            }

            while (node.board.getStatus()) {
                BoardImpl nextMove = node.board.getRandomLeagalNextMove();
                Node child = new Node(nextMove);
                child.parent = node;
                node.addChild(child);
                node = child;
            }

            return node.board.findWinner().getWinningPiece();
        }

        private void backPropagation(Piece resultPiece, Node selected) {
            // Propagate the result of the simulation back up the tree
            Node node = selected;

            while (node != null) {
                node.visit++;

                if (node.board.getPlayer() == resultPiece) {
                    node.value++;
                }
                node = node.parent;
            }
        }

        private List<BoardImpl> getAllLegalMoves(BoardImpl board) {

            // Get all legal moves for the current game board
            Piece nextPlayer = board.getPlayer() == Piece.BLUE ? Piece.GREEN : Piece.BLUE;
            List<BoardImpl> moves = new ArrayList<>();

           // outerLoop:
            for (int i = 0; i < 6; i++) {
                int raw = board.findNextAvailableSpot(i);

                if (raw != -1) {
                    BoardImpl legalMove = new BoardImpl(board.getPieces(), board.getBoardUI());
                    legalMove.updateMove(i, nextPlayer);
                    moves.add(legalMove);
                }
            }
            return moves;
        }

        private Node findBestNodeWithUCT(Node node) {
            // Find the best node to explore using the UCT (Upper Confidence Bound for Trees) formula
            int parentVisit = node.visit;
            return Collections.max(
                    node.children,
                    Comparator.comparing(c -> uctValue(parentVisit, c.value, c.visit))
            );
        }

        private double uctValue(int totalVisit, double nodeWinScore, int nodeVisit) {
            // Calculate the UCT value for a node
            if (nodeVisit == 0) {
                return Integer.MAX_VALUE;
            }
            return ((double) nodeWinScore / (double) nodeVisit)
                    + 1.41 * Math.sqrt(Math.log(totalVisit) / (double) nodeVisit);
        }

    }
}
