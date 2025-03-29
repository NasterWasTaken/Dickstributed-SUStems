package hgnn;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Controller
public class GooController {
    
    private RMIClient client;

    /**
     * Autowired Constructor for the class
     */
    @Autowired
    public GooController() {}

    /**
     * Mapping for the 1st loging, creating a new user in the process and redirecting to the homepage
     * @param modelo model class for springboot
     * @return String
     */
    @GetMapping("/")
    public String redirect(Model modelo) {
        this.client = new RMIClient();
        return "homepage";
    }

    /**
     * Function that receives the info that is input from the homepage
     * Takes care of the "search" function
     * @param query String query for the search
     * @param curPage Current page on the list of results
     * @param action Action
     * @param model model class for springboot
     * @return String
     */
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
                model.addAttribute("results", results);
                model.addAttribute("curPage", curPage);
                model.addAttribute("maxPages", maxPages);
            }
        } catch (Exception e) {
            System.out.println("[Controller:homepage] An Exception has occurred while searching: " + e);
        }

        return "homepage";
    }

    /**
     * Function that receives the info that is input from the homepage
     * Takes care of the "index" function
     * @param query String query for the search
     * @param action Action
     * @param model model class for springboot
     * @return String
     */
    @PostMapping("/homepage")
    public String index(@RequestParam("query") String query, @RequestParam("action") String action, Model model) {

        try {
            if(action.equals("index")) {
                this.client.indexURL(0, query);
            }
        } catch (Exception e) {
            System.out.println("[Controller:Index] Exception occurred when indexing url: " + e);
        }

        return "homepage";
    }

    /**
     * Redirects to admin page
     * @return String
     */
    @PostMapping("/gotoAdmin")
    public String redirectAdmin() {
        return "redirect:/admin";
    }

    /**
     * Admin page that displays the top searches and the active barrels
     * @param model model class for springboot
     * @return String
     */
    @GetMapping("/admin")
    public String admin(Model model) {
        try {
            model.addAttribute("resultsTop", this.client.tops());
            model.addAttribute("resultsBarrels", this.client.barrelLists());
        } catch (Exception e) {
            System.out.println("[Admin] Error fetching info: " + e);
        }
        return "admin";
    }
}