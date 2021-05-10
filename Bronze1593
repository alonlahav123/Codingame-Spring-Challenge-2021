import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Hashtable;

class Cell {

    int index;
    int richness;
    int[] neighbours;

    public Cell(int index, int richness, int[] neighbours) {
        this.index = index;
        this.richness = richness;
        this.neighbours = neighbours;
    }
}

class Tree {

    int cellIndex;
    int size;
    boolean isMine;
    boolean isDormant;

    public Tree(int cellIndex, int size, boolean isMine, boolean isDormant) {
        this.cellIndex = cellIndex;
        this.size = size;
        this.isMine = isMine;
        this.isDormant = isDormant;
    }
}

class Action {

    static final String WAIT = "WAIT";
    static final String SEED = "SEED";
    static final String GROW = "GROW";
    static final String COMPLETE = "COMPLETE";
    String type;
    Integer targetCellIdx;
    Integer sourceCellIdx;

    public Action(String type, Integer sourceCellIdx, Integer targetCellIdx) {
        this.type = type;
        this.targetCellIdx = targetCellIdx;
        this.sourceCellIdx = sourceCellIdx;
    }

    public Action(String type, Integer targetCellIdx) {
        this(type, null, targetCellIdx);
    }

    public Action(String type) {
        this(type, null, null);
    }

    static Action parse(String action) {
        String[] parts = action.split(" ");
        switch (parts[0]) {
            case WAIT:
                return new Action(WAIT);
            case SEED:
                return new Action(SEED, Integer.valueOf(parts[1]), Integer.valueOf(parts[2]));
            case GROW:
            case COMPLETE:
            default:
                return new Action(parts[0], Integer.valueOf(parts[1]));
        }
    }

    @Override
    public String toString() {
        if (WAIT.equalsIgnoreCase(type)) {
            return Action.WAIT;
        }
        if (SEED.equalsIgnoreCase(type)) {
            return String.format("%s %d %d", SEED, sourceCellIdx, targetCellIdx);
        }
        return String.format("%s %d", type, targetCellIdx);
    }
}

class Game {

    int day;
    int nutrients;
    List<Cell> board;
    List<Action> possibleActions;
    List<Tree> trees;
    int mySun, opponentSun;
    int myScore, opponentScore;
    boolean opponentIsWaiting;

    public Game() {
        board = new ArrayList<>();
        possibleActions = new ArrayList<>();
        trees = new ArrayList<>();
    }

    Action getNextAction() {
        Hashtable<Integer, Tree> treeLocs = new Hashtable<>();
        List<Integer> shadeLocs = new ArrayList<>(); //could be normal array?
        int[] amtTrees = {0, 0, 0, 0};
        List<Double> actionFitness = new ArrayList<>();

        //get all tree locations and finding # of seeds
        for(int i = 0; i < trees.size(); i++) {
            treeLocs.put(trees.get(i).cellIndex, trees.get(i));
            if(trees.get(i).isMine) {
                if(trees.get(i).size == 0) amtTrees[0]++;
                else if(trees.get(i).size == 1) amtTrees[1]++;
                else if(trees.get(i).size == 2) amtTrees[2]++;
                else if(trees.get(i).size == 3) amtTrees[3]++;                
            }
        }

        //calculating fitness for all possible actions
        for(int i = 0; i < possibleActions.size(); i++) {
            actionFitness.add( calcFitness(possibleActions.get(i), treeLocs, amtTrees) );
        }
        
        double highestFitness = 0;
        int highestFitnessIndex = 0;
        for(int i = 0; i < possibleActions.size(); i++) {
            if(actionFitness.get(i) > highestFitness) {
                highestFitness = actionFitness.get(i);
                highestFitnessIndex = i;
            }
        }

        return possibleActions.get(highestFitnessIndex);
    }

    double calcFitness(Action action, Hashtable<Integer, Tree> treeLocs, int[] amtTrees) {
        if(action.toString().contains("SEED")) return calcSeedFitness(action, treeLocs, amtTrees[0]);
        else if(action.toString().contains("GROW")) return calcGrowFitness(action, treeLocs, amtTrees);
        else if(action.toString().contains("COMPLETE")) return calcCompleteFitness(action);
        return -1;
    }

    double calcGrowFitness(Action action, Hashtable<Integer, Tree> treeLocs, int[] amtTrees) {
        double fitness = 0;
        int curCell = action.targetCellIdx;
        Tree tree = treeLocs.get(curCell);

        //potential sun points if grown
        fitness += ( (tree.size+1) * (24-day) );

        //does block opponent
        int opponentBlocked = 0;
        for(int i = 0; i < board.get(curCell).neighbours.length; i++) {
            int neightbourIndex = board.get(curCell).neighbours[i];

            if(treeLocs.containsKey(neightbourIndex) && !treeLocs.get(neightbourIndex).isMine) {
                opponentBlocked++;
            }
        }
        //fitness += opponentBlocked*3;

        //calc cost of growing
        int cost = 0;
        if(tree.size == 0) cost = 1+amtTrees[1]-Math.max(0, (20-day)/2);
        else if(tree.size == 1) cost = 3+amtTrees[2];
        else if(tree.size == 2) cost = 7+amtTrees[3];
        fitness -= cost;

        //good to grow in center
        if(curCell == 0) fitness += 25;

        fitness += board.get(curCell).richness/20;

        return fitness/25.0; //max is ~65
    }

    double calcCompleteFitness(Action action) {
        if(day < 14) return -1;
        if(day >= 14 && day < 22) return -0.2;
        if(day > 22) return 5+board.get(action.targetCellIdx).richness;


        return 0;
    }

    // if seedpos = treepos VERY bad
    // fitness = richness - potential shade
    double calcSeedFitness(Action action, Hashtable<Integer, Tree> treeLocs, int amt0Trees) {
        double fitness = 0;
        int curCell = action.targetCellIdx;

        //check if there is a tree at the seed pos
        if(treeLocs.containsKey(curCell)) {
            return Integer.MIN_VALUE;
        }
        
        //add richness
        if(board.get(curCell).richness == 0) { //if unusable
            return Integer.MIN_VALUE;
        } else {
            fitness += (board.get(curCell).richness)*3;
        }

        //if center of board, very good spot
        if(curCell == 0 && day < 20) return 5;

        //how many trees cast shade on lot (just looks if there are trees on neighboring indexes)
        int amtShade = 0;
        for(int i = 0; i < board.get(curCell).neighbours.length; i++) {
            int neightbourIndex = board.get(curCell).neighbours[i];

            if(treeLocs.containsKey(neightbourIndex)) { //bad cause it includes seeds
                amtShade++;
            }
        }
        fitness += 10-(amtShade*3);

        //cost of action
        fitness -= 3*amt0Trees;

        fitness += (24-day)*10/24;
        //max = 24+10 = 34
        return fitness/22.0;
    }
}

class Player {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);

        Game game = new Game();

        int numberOfCells = in.nextInt();
        for (int i = 0; i < numberOfCells; i++) {
            int index = in.nextInt();
            int richness = in.nextInt();
            int neigh0 = in.nextInt();
            int neigh1 = in.nextInt();
            int neigh2 = in.nextInt();
            int neigh3 = in.nextInt();
            int neigh4 = in.nextInt();
            int neigh5 = in.nextInt();
            int[] neighs = new int[]{neigh0, neigh1, neigh2, neigh3, neigh4, neigh5};
            Cell cell = new Cell(index, richness, neighs);
            game.board.add(cell);
        }

        while (true) {
            game.day = in.nextInt();
            game.nutrients = in.nextInt();
            game.mySun = in.nextInt();
            game.myScore = in.nextInt();
            game.opponentSun = in.nextInt();
            game.opponentScore = in.nextInt();
            game.opponentIsWaiting = in.nextInt() != 0;

            game.trees.clear();
            int numberOfTrees = in.nextInt();
            for (int i = 0; i < numberOfTrees; i++) {
                int cellIndex = in.nextInt();
                int size = in.nextInt();
                boolean isMine = in.nextInt() != 0;
                boolean isDormant = in.nextInt() != 0;
                Tree tree = new Tree(cellIndex, size, isMine, isDormant);
                game.trees.add(tree);
            }

            game.possibleActions.clear();
            int numberOfPossibleActions = in.nextInt();
            in.nextLine();
            for (int i = 0; i < numberOfPossibleActions; i++) {
                String possibleAction = in.nextLine();
                game.possibleActions.add(Action.parse(possibleAction));
            }

            Action action = game.getNextAction();
            System.out.println(action);
        }
    }
}
