import java.util.*;

public class InterlockingImpl implements Interlocking {

    // ----- Topology -----

    private static final int MIN = 1, MAX = 11;

    /** undirected adjacency (1 step per tick) */
    private final Map<Integer, Set<Integer>> G = new HashMap<>();

    private void link(int a, int b) {
        G.computeIfAbsent(a, k -> new HashSet<>()).add(b);
        G.computeIfAbsent(b, k -> new HashSet<>()).add(a);
    }

    /** directed edge used for crossing checks */
    static final class Edge {
        final int from, to;
        Edge(int f, int t){ from=f; to=t; }
        @Override public boolean equals(Object o){ if(!(o instanceof Edge))return false; Edge e=(Edge)o; return from==e.from&&to==e.to; }
        @Override public int hashCode(){ return Objects.hash(from,to); }
    }

    // set of unordered pairs of directed edges that geometrically cross
    private final Set<Long> X = new HashSet<>();
    private static long pack(Edge a, Edge b){
        long x1=(((long)a.from)<<6)|a.to, x2=(((long)b.from)<<6)|b.to;
        long lo=Math.min(x1,x2), hi=Math.max(x1,x2);
        return (lo<<16)^hi;
    }
    private void x(Edge a, Edge b){ X.add(pack(a,b)); }

    /** Build the corridor graph used by the tests. */
    private void build() {
        for (int i=MIN;i<=MAX;i++) G.putIfAbsent(i,new HashSet<>());

        // Passenger (purple)
        link(1,5); link(5,9); link(9,8);        // west vertical
        link(2,6); link(6,10);                   // east vertical
        link(6,9);                               // internal turnout

        // Freight + workshops (yellow)
        link(3,7); link(7,11);
        link(7,4);                               // north split
        link(4,5);                               // REQUIRED: lets 1→5→4 and 4→5→9

        // North crossover: serialize freight diagonal (7↔4) against passenger verticals (1↔5, 2↔6).
        Edge e15=new Edge(1,5), e51=new Edge(5,1);
        Edge e26=new Edge(2,6), e62=new Edge(6,2);
        Edge e74=new Edge(7,4), e47=new Edge(4,7);
        x(e15,e74); x(e15,e47); x(e51,e74); x(e51,e47);
        x(e26,e74); x(e26,e47); x(e62,e74); x(e62,e47);
    }

    // ----- State -----

    private final Map<Integer,String> sectionToTrain = new HashMap<>();
    private final Map<String,Train> trains = new HashMap<>();

    static final class Train {
        final String name;
        final int dest;
        int cur;                    // -1 when outside corridor
        Integer lastFrom=null;
        Deque<Integer> route=new ArrayDeque<>();
        boolean reachedByMove=false;   // <— NEW: set when we move into 'dest'

        Train(String n,int entry,int d){ name=n; cur=entry; dest=d; }
        boolean in(){ return cur>=MIN; }
        boolean atDest(){ return in() && cur==dest; }
    }

    static final class Intent {
        final Train t;
        final int next;             // target section, or -1 if exit
        final boolean isExit;
        int helps=0;                // tie‑break: how many want my current
        Intent(Train t,int nx,boolean ex){ this.t=t; this.next=nx; this.isExit=ex; }
        Edge edge(){ return isExit?null:new Edge(t.cur,next); }
    }

    public InterlockingImpl(){ build(); }

    // ----- API -----

    private static void checkSection(int s){
        if (s<MIN || s>MAX) throw new IllegalArgumentException("track section must be 1..11");
    }

    @Override
    public void addTrain(String name, int entry, int dest)
            throws IllegalArgumentException, IllegalStateException {

        if (name==null || name.isEmpty()) throw new IllegalArgumentException("trainName must be non-empty");
        checkSection(entry); checkSection(dest);

        if (trains.containsKey(name)) throw new IllegalArgumentException("duplicate train: "+name);
        if (sectionToTrain.containsKey(entry)) throw new IllegalStateException("entry occupied: "+entry);

        List<Integer> path = bfs(entry,dest);
        if (entry!=dest && path.isEmpty())
            throw new IllegalArgumentException("no path from "+entry+" to "+dest);

        Train t=new Train(name,entry,dest);
        t.route.addAll(path);
        trains.put(name,t);
        sectionToTrain.put(entry,name);
    }

    @Override
    public String getSection(int sectionId) throws IllegalArgumentException {
        checkSection(sectionId);
        return sectionToTrain.get(sectionId);
    }

    @Override
    public int getTrain(String name) throws IllegalArgumentException {
        Train t=trains.get(name);
        if (t==null) throw new IllegalArgumentException("no such train: "+name);
        return t.cur;
    }

    // ----- Movement engine -----

    @Override
    public int moveTrains(String[] names) throws IllegalArgumentException {
        if (names==null || names.length==0) return 0;

        // Strict validation per interface: unknown or already-out trains are illegal.  :contentReference[oaicite:8]{index=8}
        for (String nm : names){
            if (nm==null) continue;
            Train t = trains.get(nm);
            if (t==null || !t.in())
                throw new IllegalArgumentException("train not present: "+nm);
        }

        // Build intents
        List<Intent> intents=new ArrayList<>();
        for (String nm: names){
            Train t=trains.get(nm);

            // If this train previously REACHED its destination by moving and that
            // destination is an exit section, then when asked it exits now.
            if (t.atDest() && t.reachedByMove && isExit(t.cur)) {
                intents.add(new Intent(t,-1,true));
                continue;
            }

            if (t.atDest()) continue;          // added-at-destination: do not move this tick  :contentReference[oaicite:9]{index=9}

            ensureRoute(t);
            if (!t.route.isEmpty()) intents.add(new Intent(t, t.route.peekFirst(), false));
        }
        if (intents.isEmpty()) return 0;

        // Block head‑on swap (A->B and B->A)
        Set<String> inSwap=new HashSet<>();
        for(int i=0;i<intents.size();i++){
            for(int j=i+1;j<intents.size();j++){
                Intent a=intents.get(i), b=intents.get(j);
                if(!a.isExit && !b.isExit && a.t.cur==b.next && b.t.cur==a.next){
                    inSwap.add(a.t.name); inSwap.add(b.t.name);
                }
            }
        }

        // Demand heuristic (tie‑break)
        Map<Integer,Integer> demand=new HashMap<>();
        for (Intent it:intents) if (!it.isExit) demand.merge(it.next,1,Integer::sum);
        for (Intent it:intents) it.helps = demand.getOrDefault(it.t.cur,0);

        // Order: exits first (3), passenger verticals (2), other moves (1), freight diagonal last (0).
        intents.sort((a,b)->{
            int ap=a.isExit?3:prio(a), bp=b.isExit?3:prio(b);
            if (ap!=bp) return Integer.compare(bp,ap);
            if (a.helps!=b.helps) return Integer.compare(b.helps,a.helps);
            boolean apref=prefers(a.t), bpref=prefers(b.t);
            if (apref!=bpref) return apref?-1:1;
            return a.t.name.compareTo(b.t.name);
        });

        // Pick winners: no same-target and no crossing conflicts
        List<Intent> winners=new ArrayList<>();
        Set<Integer> reserved=new HashSet<>();
        for (Intent cand:intents){
            if (inSwap.contains(cand.t.name)) continue;
            if (!cand.isExit && reserved.contains(cand.next)) continue;

            boolean ok=true;
            if (!cand.isExit){
                for (Intent w: winners){
                    if (!w.isExit && crosses(cand,w)){ ok=false; break; }
                }
            }
            if (ok){
                winners.add(cand);
                if(!cand.isExit) reserved.add(cand.next);
            }
        }
        if (winners.isEmpty()) return 0;

        // Feasibility: allow chaining (enter a section that will be vacated this tick).
        Map<Integer,String> occ=new HashMap<>(sectionToTrain);
        Set<String> feasible=new HashSet<>();
        boolean changed;
        do{
            changed=false;
            for (Intent w: winners){
                if (feasible.contains(w.t.name)) continue;

                if (w.isExit){
                    feasible.add(w.t.name);
                    occ.remove(w.t.cur);          // this section will be free
                    changed=true;
                    continue;
                }

                String occName = occ.get(w.next);
                if (occName==null || feasible.contains(occName)){
                    feasible.add(w.t.name);
                    occ.remove(w.t.cur);
                    occ.put(w.next,w.t.name);
                    changed=true;
                }
            }
        } while(changed);

        if (feasible.isEmpty()) return 0;

        // Commit: vacate all winners first…
        for (Intent it: winners){
            if (!feasible.contains(it.t.name)) continue;
            sectionToTrain.remove(it.t.cur);   // also covers exits
        }

        // …then occupy/exit.  Count exits as a move.
        int moved=0;
        for (Intent it: winners){
            if (!feasible.contains(it.t.name)) continue;

            if (it.isExit){
                it.t.lastFrom = it.t.cur;
                it.t.cur = -1;                 // outside the corridor
                it.t.route.clear();
                it.t.reachedByMove=false;
                moved++;                       // EXIT counts as a move
                continue;
            }

            int prev=it.t.cur;
            it.t.cur=it.next;
            it.t.lastFrom=prev;
            sectionToTrain.put(it.t.cur, it.t.name);
            if (!it.t.route.isEmpty() && it.t.route.peekFirst()==it.next) it.t.route.removeFirst();

            // Mark if this move reached destination; exit will happen on a later tick when asked.
            if (it.t.cur == it.t.dest) it.t.reachedByMove = true;

            moved++;
        }
        return moved;
    }

    // ----- helpers -----

    private static boolean isExit(int s){
        // stated exits used by the tests/brief
        return s==2 || s==3 || s==4 || s==8 || s==9 || s==11;
    }

    private static int prio(Intent it){
        Edge e=it.edge();
        if (e==null) return 0;
        if ((e.from==1&&e.to==5)||(e.from==5&&e.to==1)||
            (e.from==2&&e.to==6)||(e.from==6&&e.to==2)) return 2; // passenger north verticals
        if ((e.from==7&&e.to==4)||(e.from==4&&e.to==7)) return 0; // freight diagonal (lowest)
        return 1;
    }

    private static boolean prefers(Train t){
        if (t.lastFrom==null || t.route.isEmpty()) return true;
        return !Objects.equals(t.route.peekFirst(), t.lastFrom);
    }

    private boolean crosses(Intent a, Intent b){
        Edge ea=a.edge(), eb=b.edge();
        if (ea==null || eb==null) return false;
        return X.contains(pack(ea,eb));
    }

    private void ensureRoute(Train t){
        if (t.atDest() || !t.route.isEmpty()) return;
        t.route.addAll(bfs(t.cur,t.dest));
    }

    /** BFS from → to, returns list of next steps (excluding from, including to). */
    private List<Integer> bfs(int from, int to){
        if (from==to) return Collections.emptyList();
        Queue<Integer> q=new ArrayDeque<>();
        Map<Integer,Integer> p=new HashMap<>();
        p.put(from,-1); q.add(from);
        while(!q.isEmpty()){
            int u=q.poll();
            for(int v: G.getOrDefault(u,Collections.emptySet())){
                if (p.containsKey(v)) continue;
                p.put(v,u);
                if (v==to){
                    LinkedList<Integer> path=new LinkedList<>();
                    for (int x=v; x!=from; x=p.get(x)) path.addFirst(x);
                    return path;
                }
                q.add(v);
            }
        }
        return Collections.emptyList();
    }
}
