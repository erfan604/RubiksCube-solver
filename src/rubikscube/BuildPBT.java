package rubikscube;

public class BuildPBT {
    public static void main(String[] args) {
        MoveTables.init();
        PruningTables.buildAllBlocking();
        PruningTables.saveToDisk();
        System.out.println("Pruning tables built and saved to pbt.cache");
    }
}
