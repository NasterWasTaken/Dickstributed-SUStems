package hgnn;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Controller
public class GooController {
    
    private RMIClient client;
    private static final Logger log = LoggerFactory.getLogger(GooController.class);

    @Autowired
    public GooController() {
        
    }

    @GetMapping("/")
    public String redirect(Model modelo) {
        this.client = new RMIClient();
        return "homepage";
    }

    @GetMapping("/homepage")
    public String homePage(@RequestParam("query") String query, @RequestParam(value = "page", defaultValue = "1") int curPage, @RequestParam("action") String action, Model model) {
        try {
            if(action.equals("search")) {
                ArrayList<Webpage> results = this.client.search(0, query);

                int resultsPage = 10;
                int total = results.size();
                int maxPages = (int) Math.ceil((double) total / resultsPage);

                int start = (curPage - 1) * resultsPage;
                int end = Math.min(start + resultsPage, total);
                ArrayList<Webpage> curPageList = new ArrayList<>();

                for(int i = start; i < end; ++i) {
                    curPageList.add(results.get(i));
                }

                model.addAttribute("query", query);
                model.addAttribute("action", action);
                model.addAttribute("results", curPageList);
                model.addAttribute("curPage", curPage);
                model.addAttribute("maxPages", maxPages);
            }
        } catch (Exception e) {
            System.out.println("[Controller:homepage] An Exception has occurred while searching " + e);
        }

        return "homepage";
    }

    @GetMapping("/admin")
    public String adminPage() {
        return "admin";
    }
}