package games;

import main.Database;
import main.Goldilocks;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class GameOf2048 {

    private Random random = new Random();
    private Member player;
    private Message gameMessage;
    private Message scoreMessage;
    private int[][] gameBoard;
    private int score = 0;
    private int highScore;
    private User highScoreUser;
    private int size;

    public GameOf2048(TextChannel textChannel, Member player, int size, boolean fromSave, String boardInfo) {

        this.player = player;
        this.size = size;
        gameBoard = new int[size][size];
        if (!fromSave) initializeBoard();
        else deSerialize(boardInfo);

        String[] highScoreInfo = Database.get2048Hs();
        highScoreUser = Goldilocks.jda.getUserById(highScoreInfo[0]);
        highScore = Integer.parseInt(highScoreInfo[1]);
        //rotateBoard(new int[][]{{0,1,2,3},{4,5,6,7},{8,9,10,11},{12,13,14,15}});

        //Send board
        scoreMessage = textChannel.sendMessage( "**__" + player.getEffectiveName() + "'s Game of 2048__**\n**High Score: **" + highScore + " by " + highScoreUser.getName()).complete();
        gameMessage = textChannel.sendMessage(renderBoardString()).complete();
        Goldilocks.activeGames.put(textChannel.getGuild(), gameMessage);

        reactionController();
        gameMessage.addReaction("◀").queue();
        gameMessage.addReaction("🔼").queue();
        gameMessage.addReaction("▶").queue();
        gameMessage.addReaction("🔽").queue();
        gameMessage.addReaction("📥").queue();
        gameMessage.addReaction("❌").queue();
    }

    private void initializeBoard() {
        int numsLeft = 2;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                gameBoard[i][j] = 0;
            }
        }
        //gameBoard = new int[][]{{0, 8, 4, 4}, {4, 16, 4, 16}, {16, 4, 16, 4}, {0, 0, 0, 0}}; //Test board
        for (int i = 0; i < numsLeft; i++) addPiece();
    }

    public String serializeBoard() {
        String boardString = "";
        for (int i = 0; i < gameBoard.length; i++) {
            for (int j = 0; j < gameBoard[0].length; j++) {
                boardString += gameBoard[i][j] + " ";
            }
        }
        boardString += score;
        return boardString;
    }

    public void deSerialize(String boardInfo) {
        String[] boardNums = boardInfo.split(" ");
        size = (int) Math.round(Math.sqrt(boardNums.length - 1));
        int index = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                gameBoard[i][j] = Integer.parseInt(boardNums[index++]);
            }
        }
        score = Integer.parseInt(boardNums[index]);
    }

    private void addPiece() {
        boolean filled = false;
        int pieceValue = 2;
        if (random.nextInt(8) > 6) pieceValue = 4;
        while (!filled) {
            int randomIndex = random.nextInt(size * size);
            if (gameBoard[randomIndex / gameBoard.length][randomIndex % gameBoard[0].length] == 0) {
                gameBoard[randomIndex / gameBoard.length][randomIndex % gameBoard[0].length] = pieceValue;
                filled = true;
            }
        }
    }

    private boolean hasLost() {
        int[][] currentBoard = gameBoard;
        for (int i = 0; i < 4; i++) {
            currentBoard = rotateBoard(currentBoard, i);
            for (int j = 0; j < gameBoard[0].length; j++) {
                for (int k = 0; k < gameBoard.length; k++) {
                    if (canMoveRight(j * gameBoard.length + k, currentBoard)) return false;
                }
            }
        }
        return true;
    }

    private boolean isFilled() {
        for (int i = 0; i < gameBoard[0].length; i++) {
            for (int j = 0; j < gameBoard.length; j++) {
                if (gameBoard[i][j] == 0) return false;
            }
        }
        return true;
    }

    private void reactionController() {
        Goldilocks.eventWaiter.waitForEvent(GuildMessageReactionAddEvent.class, e -> {
            return e.getMember().equals(player) && e.getMessageId().equals(gameMessage.getId())
                    && e.getReactionEmote().isEmoji() && ("🔼🔽▶◀📥❌").contains(e.getReactionEmote().getEmoji());
        }, e -> {
            String emote = e.getReactionEmote().getEmoji();

            if (("🔼🔽▶◀").contains(emote)) {
                int[][] beforeBoard = gameBoard;
                if (("🔽").equals(emote)) {
                    rotateBoard(3);
                    moveRight();
                    rotateBoard(1);
                }
                if (("🔼").equals(emote)) {
                    rotateBoard(1);
                    moveRight();
                    rotateBoard(3);
                }
                if (("▶").equals(emote)) {
                    rotateBoard(4);
                    moveRight();
                }
                if (("◀").equals(emote)) {
                    rotateBoard(2);
                    moveRight();
                    rotateBoard(2);
                }
                if (validMove(beforeBoard)) {
                    addPiece();
                    renderBoard(e.getReaction(), e.getUser());
                } else e.getReaction().removeReaction(e.getUser()).queue();

                if (isFilled() && hasLost()) {
                    Database.add2048Score(player.getId(), score, true, serializeBoard());
                    scoreMessage.editMessage("**__" + player.getEffectiveName() + "'s Game of 2048__**\n**Final Score: **\n" + renderBoardString()).complete();
                    gameMessage.delete().queue();
                    Goldilocks.activeGames.remove(player.getGuild());
                    return;
                }
                reactionController();
            }

            if (("❌📥").contains(emote)) {
                Goldilocks.activeGames.remove(player.getGuild());
                Database.add2048Score(player.getId(), score, ("❌").equals(emote), serializeBoard());
                scoreMessage.editMessage("**__" + player.getEffectiveName() + "'s Game of 2048__**\n**Final Score: **\n" + renderBoardString()).queue();
                gameMessage.delete().queue();
            }

        }, 5L, TimeUnit.MINUTES, () -> {
            Goldilocks.activeGames.remove(player.getGuild());
            gameMessage.delete().queue();
            scoreMessage.delete().queue();
            Database.add2048Score(player.getId(), score, false, serializeBoard());
        });
    }

    private void renderBoard(MessageReaction reaction, User user) {
        gameMessage.editMessage(renderBoardString()).submit().thenRun(() -> reaction.removeReaction(user).queue());
    }

    private String renderBoardString() {

        String boardString = renderScoreString() + "\n";
        for (int i = 0; i < gameBoard.length; i++) {
            for (int j = 0; j < gameBoard[0].length; j++) {
                boardString += Goldilocks.jda.getEmotesByName(String.valueOf(gameBoard[i][j]).length() == 1 ? gameBoard[i][j] + "_" : String.valueOf(gameBoard[i][j]), true).get(0).getAsMention();
            }
            boardString += "\n";
        }
        boardString += "";
        return boardString;
    }

    private String renderScoreString() {
        String scoreString = "";

        String scoreS = String.format("%04d", score);
        for (char c : scoreS.toCharArray()) {
            scoreString += Goldilocks.jda.getEmotesByName("num" + c, true).get(0).getAsMention();
        }
        return scoreString;
    }

    private void moveRight() {
        int rowIndex = 0;
        for (int[] row : gameBoard) {
            boolean lastPieceReplaced = false;
            for (int i = row.length - 2; i >= 0; i--) {
                if (row[i] != 0) {
                    int currentValue = row[i];
                    row[i] = 0;
                    int j = i;
                    while (j + 1 < gameBoard[0].length && canMoveRight(rowIndex * gameBoard.length + j, currentValue)) j++;
                    if (row[j] == 0) {
                        gameBoard[rowIndex][j] = currentValue;
                        lastPieceReplaced = false;
                    }
                    else {
                        if (!lastPieceReplaced) {
                            gameBoard[rowIndex][j] = 2 * currentValue;
                            score += 2 * currentValue;
                            lastPieceReplaced = true;
                        } else {
                            gameBoard[rowIndex][j - 1] = currentValue;
                            lastPieceReplaced = false;
                        }

                    };
                }
            }
            rowIndex++;
        }
    }

    private boolean canMoveRight(int index, int value) {
        int row = index / gameBoard.length;
        int col = index % gameBoard[0].length;

        if (col == gameBoard[0].length) return false;
        if (col + 1 < gameBoard[0].length) {
            return (gameBoard[row][col + 1] == 0) || gameBoard[row][col + 1] == value;
        }
        return false;

    }

    private boolean canMoveRight(int index, int[][] gameBoard) {
        int row = index / gameBoard.length;
        int col = index % gameBoard[0].length;

        if (col == gameBoard[0].length) return false;
        if (col + 1 < gameBoard[0].length) {
            return (gameBoard[row][col + 1] == 0) || gameBoard[row][col + 1] == gameBoard[row][col];
        }
        return false;

    }

    private void rotateBoard(int numTimes) {
        int result[][] = gameBoard;
        for (int i = 0; i < numTimes; i ++) result = rotateBoard(result);
        gameBoard = result;
    }

    private int[][] rotateBoard(int[][] arr, int numTimes) {
        int result[][] = arr;
        for (int i = 0; i < numTimes; i++) result = rotateBoard(result);
        return result;
    }

    private int[][] rotateBoard(int[][] mat) {
        int result[][] = new int[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                result[i][j] = 0;
            }
        }

        for (int i = 0; i < mat[0].length; i++) {
            for (int j = 0; j < mat.length; j++) {
                result[j][mat.length - 1 - i] = mat[i][j];
            }
        }
        return result;
    }

    private void printArray(int[][] arr) {
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[0].length; j++) {
               System.out.print(arr[i][j] + " ");
            }
            System.out.print("\n");
        }
        System.out.println();
    }

    private boolean validMove(int[][] previousBoard) {
        for (int i = 0; i < previousBoard.length; i++) {
            for (int j = 0; j < previousBoard[0].length; j++) {
                if (previousBoard[i][j] != gameBoard[i][j]) return true;
            }
        }
        return false;
    }

}
