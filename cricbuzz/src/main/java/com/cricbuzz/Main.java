/*
 * Copyright: ThoughtSpot Inc. 2025
 */

package com.cricbuzz;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CLASS_DEFINITION_COMMENTS
 *
 * @author Mayank Sharma (mayank.sharma@thoughtspot.com)
 */
public class Main {
    public static void main(String[] args) {
        int n = 2;
        List<Integer> in = new ArrayList(n);
        Queue<Integer> q = new LinkedList<>();
        ExecutorService service = Executors.newFixedThreadPool(10);
        while(!q.isEmpty()) {
            Integer v = q.peek();
            service.submit(execute);
            nfor (Integer x: adj[v]) {
                if (in[x] == 0) {
                    q.push(x);
                }
            }
        }
    }
    void execute(Integer v, dep) {
        in[all childs of v]--;
    }
}
