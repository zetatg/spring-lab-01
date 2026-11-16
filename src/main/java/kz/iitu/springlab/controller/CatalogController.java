package kz.iitu.springlab.controller;

import kz.iitu.springlab.aspect.CallCounterAspect;
import kz.iitu.springlab.service.CatalogService;
import org.springframework.aop.support.AopUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class CatalogController {

    private final CatalogService catalogService;
    private final CallCounterAspect counter;

    public CatalogController(CatalogService catalogService, CallCounterAspect counter) {
        this.catalogService = catalogService;
        this.counter = counter;
    }

    @GetMapping("/api/lab4/item/{id}")
    public String findById(@PathVariable long id) {
        return catalogService.findById(id);
    }

    @GetMapping("/api/lab4/items")
    public List<String> findAll(
            @RequestParam(defaultValue = "5") int limit) {
        return catalogService.findAll(limit);
    }

    @DeleteMapping("/api/lab4/item/{id}")
    public String remove(@PathVariable long id) {
        return catalogService.remove(id);
    }

    @GetMapping("/api/lab4/proxy")
    public Map<String, String> proxyInfo() {
        return Map.of(
                "className", catalogService.getClass().getName(),
                "superClass",
                catalogService.getClass()
                        .getSuperclass()
                        .getSimpleName(),
                "isAopProxy",
                String.valueOf(
                        AopUtils.isAopProxy(catalogService)),
                "isCglib",
                String.valueOf(
                        AopUtils.isCglibProxy(catalogService))
        );
    }

    /** After the fix: 2 audit records for the inner remove() calls. */
    @GetMapping("/api/lab4/remove-twice/{id}")
    public String removeTwice(@PathVariable long id) {
        return catalogService.removeTwice(id);
    }

    /** Before the fix, kept for the demonstration: 0 audit records for the inner calls. */
    @GetMapping("/api/lab4/remove-twice-this/{id}")
    public String removeTwiceViaThis(@PathVariable long id) {
        return catalogService.removeTwiceViaThis(id);
    }

    /** Individual variant 1: calls per service method since start-up. */
    @GetMapping("/api/lab4/stats")
    public Map<String, Integer> stats() {
        return counter.getStatistics();
    }
}