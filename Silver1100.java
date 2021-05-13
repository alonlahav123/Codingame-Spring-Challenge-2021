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

class RankedAction {
    Action action;
    double fitness;

    RankedAction(Action a, double f) {
        this.action = a;
        this.fitness = f;
    }
}

class ActionCosts {
    int plantSeed;
    int[] growCosts = {0, 0, 0};
    int complete = 4;
    int[] amtTrees = {0, 0, 0, 0}; // { # lvl 0 trees, # lvl 1 trees, # lvl 2 trees, # lvl 3 trees}

    ActionCosts(List<Tree> trees) {
        calcCosts(trees);
    }

    public void calcCosts(List<Tree> trees) {
        for(int i = 0; i < trees.size(); i++) {
            if(trees.get(i).isMine) {
                amtTrees[trees.get(i).size]++;
            }
        }

        this.plantSeed = amtTrees[0];
        this.growCosts[0] = 1 + amtTrees[1];
        this.growCosts[1] = 3 + amtTrees[2];
        this.growCosts[2] = 7 + amtTrees[3];
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
        ActionCosts costs = new ActionCosts(trees);

        if(day >= 22) {
            for(int i = 0; i < possibleActions.size(); i++) {
                if(possibleActions.get(i).type.equals("COMPLETE")) {
                    return possibleActions.get(i);
                }
            }
        }

        //get all tree locations and finding # of seeds
        for(int i = 0; i < trees.size(); i++) {
            treeLocs.put(trees.get(i).cellIndex, trees.get(i));
        }

        //find best seed location
        RankedAction actionSeed = bestSeedLocation(treeLocs);

        //find best grow action
        RankedAction actionGrow = bestGrowLocation(treeLocs, costs);
        if(costs.amtTrees[0] >= 2 && actionGrow.fitness != -1) {
            actionGrow.fitness += 0.5;
        }

        //find best grow action
        RankedAction actionComplete = bestCompleteLocation(treeLocs, costs);

        System.err.println(possibleActions);
        if(actionSeed.fitness == -1 && actionGrow.fitness == -1 && actionComplete.fitness == -1) {
            return possibleActions.get(0);
        }

        return highestFitnessAction(actionSeed, actionGrow, actionComplete);     
    }

    private Action highestFitnessAction(RankedAction actionSeed, RankedAction actionGrow, RankedAction actionComplete) {
        if(actionSeed.fitness >= actionGrow.fitness) {
            if(actionSeed.fitness >= actionComplete.fitness) {
                return actionSeed.action;
            } else {
                return actionComplete.action;
            }
        } else {
            if(actionGrow.fitness >= actionComplete.fitness) {
                return actionGrow.action;
            } else {
                return actionComplete.action;
            }
        }
    }

    private RankedAction bestCompleteLocation(Hashtable<Integer, Tree> treeLocs, ActionCosts costs) {
        int bestCompleteIndex = -1;
        double bestCompleteFitness = 0;

        for(int i = 0; i < possibleActions.size(); i++) {
            if(possibleActions.get(i).type.equals("COMPLETE")) {
                double curFitness = calcCompleteFitness(possibleActions.get(i), treeLocs, costs);

                if(bestCompleteFitness < curFitness) {
                    bestCompleteFitness = curFitness;
                    bestCompleteIndex = i;
                }
            }
        }

        if(bestCompleteIndex != -1) {
            return new RankedAction(possibleActions.get(bestCompleteIndex), bestCompleteFitness);
        }
        //if there are no grow options:
        return new RankedAction(null, -1);
    }

    private double calcCompleteFitness(Action action, Hashtable<Integer, Tree> treeLocs, ActionCosts costs) {
        int curCell = action.targetCellIdx;

        int futureSunPoints = 23-day; //modified to make it easier to work with
        int richness = board.get(curCell).richness;
        int shade = calcShadeOnIndex(curCell, treeLocs);
        int amt = costs.amtTrees[3];

        double fitness = 2.0/Math.max(0.2, ((futureSunPoints/5.0)-(amt/2)+3.0) ); //does not work 

        return fitness;
    }

    private RankedAction bestGrowLocation(Hashtable<Integer, Tree> treeLocs, ActionCosts costs) { //can combine with bestSeedLocation
        int bestGrowIndex = -1;
        double bestGrowFitness = 0;

        for(int i = 0; i < possibleActions.size(); i++) {
            if(possibleActions.get(i).type.equals("GROW")) {
                double curFitness = calcGrowFitness(possibleActions.get(i), treeLocs, costs);

                if(bestGrowFitness < curFitness) {
                    bestGrowFitness = curFitness;
                    bestGrowIndex = i;
                }
            }
        }

        if(bestGrowIndex != -1) {
            return new RankedAction(possibleActions.get(bestGrowIndex), bestGrowFitness);
        }
        //if there are no grow options:
        return new RankedAction(null, -1);
    }

    private double calcGrowFitness(Action action, Hashtable<Integer, Tree> treeLocs, ActionCosts costs) {
        int curCell = action.targetCellIdx;

        int richness = board.get(curCell).richness; //= 0 to 3
        int treeSize = treeLocs.get(curCell).size;
        int cost = costs.growCosts[treeSize];
        int shade = calcShadeOnIndex(curCell, treeLocs);

        double fitness = 1.0/(shade-(1.0*richness)+(costs.amtTrees[treeSize+1]/3)+1.0); //should probs change this

        return fitness;
    }


    private RankedAction bestSeedLocation(Hashtable<Integer, Tree> treeLocs) {
        if(day < 2) return new RankedAction(null, -1);

        int bestSeedIndex = -1;
        double bestSeedFitness = 0;
        
        System.err.println(possibleActions);

        for(int i = 0; i < possibleActions.size(); i++) {
            if(possibleActions.get(i).type.equals("SEED")) {
                double curFitness = calcSeedFitness(possibleActions.get(i), treeLocs);

                if(bestSeedFitness < curFitness) {
                    bestSeedFitness = curFitness;
                    bestSeedIndex = i;
                }
            }
        }

        if(bestSeedIndex != -1) {
            return new RankedAction(possibleActions.get(bestSeedIndex), bestSeedFitness);
        }
        //if there are no seed options:
        return new RankedAction(null, -1);
    }

    private double calcSeedFitness(Action action, Hashtable<Integer, Tree> treeLocs) {
        int curCell = action.targetCellIdx;
        int shade = calcShadeOnIndex(curCell, treeLocs);
        int richness = board.get(curCell).richness;

        
        return 2.0/(shade+3.0-(richness/8.0));
    }

    private int calcShadeOnIndex(int curCell, Hashtable<Integer, Tree> treeLocs) {
        int potentialTreeShade = 0; //potential shade once tree is grown
        int realTreeShade = 0; //current shade

        Cell cell = board.get(curCell);
        for(int i = 0; i < 6; i++) {
            int neighbourCellIndex = cell.neighbours[i];
            for(int j = 1; j <= 3; j++) {
                if(neighbourCellIndex == -1) break;

                if(treeLocs.containsKey(neighbourCellIndex)) {
                    Tree tree = treeLocs.get(neighbourCellIndex);

                    if(tree.size >= j) {
                        realTreeShade++;
                        break;
                    } else {
                        //potentialTreeShade++;
                    }
                }
                neighbourCellIndex = board.get(neighbourCellIndex).neighbours[i];
            }
        }

        return realTreeShade;
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
