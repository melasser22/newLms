package com.ejada.gateway.graphql;

import com.ejada.gateway.config.GatewayGraphqlProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Performs a lightweight structural analysis of GraphQL queries to estimate depth, breadth and
 * complexity. This avoids forwarding expensive queries to downstream services.
 */
@Component
public class GraphqlQueryAnalyzer {

  private static final Set<String> KEYWORDS = Set.of(
      "query",
      "mutation",
      "subscription",
      "fragment",
      "on",
      "true",
      "false",
      "null");

  public Analysis analyze(String query) {
    if (!StringUtils.hasText(query)) {
      return new Analysis(0, 0, 0);
    }

    int depth = 0;
    int maxDepth = 0;
    Map<Integer, Integer> breadthPerLevel = new HashMap<>();
    int complexity = 0;

    boolean inString = false;
    char stringDelimiter = 0;

    char[] chars = query.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];
      if (inString) {
        if (c == stringDelimiter && (i == 0 || chars[i - 1] != '\\')) {
          inString = false;
        }
        continue;
      }
      if (c == '\"' || c == '\'') {
        inString = true;
        stringDelimiter = c;
        continue;
      }
      if (c == '#') {
        while (i < chars.length && chars[i] != '\n') {
          i++;
        }
        continue;
      }
      if (c == '{') {
        depth++;
        if (depth > maxDepth) {
          maxDepth = depth;
        }
        breadthPerLevel.putIfAbsent(depth, 0);
        continue;
      }
      if (c == '}') {
        depth = Math.max(0, depth - 1);
        continue;
      }
      if (Character.isLetter(c) || c == '_') {
        int start = i;
        while (i + 1 < chars.length && (Character.isLetterOrDigit(chars[i + 1]) || chars[i + 1] == '_')) {
          i++;
        }
        String token = new String(chars, start, i - start + 1);
        if (KEYWORDS.contains(token)) {
          continue;
        }
        complexity++;
        int level = Math.max(depth, 1);
        breadthPerLevel.merge(level, 1, Integer::sum);
      }
    }

    int breadth = breadthPerLevel.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    return new Analysis(maxDepth, breadth, complexity);
  }

  public void assertWithinLimits(String query, GatewayGraphqlProperties properties) {
    Analysis analysis = analyze(query);
    if (analysis.depth() > properties.getMaxDepth()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "GraphQL query depth exceeds limit of " + properties.getMaxDepth());
    }
    if (analysis.breadth() > properties.getMaxBreadth()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "GraphQL query breadth exceeds limit of " + properties.getMaxBreadth());
    }
    if (analysis.complexity() > properties.getMaxComplexity()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "GraphQL query complexity exceeds limit of " + properties.getMaxComplexity());
    }
  }

  public record Analysis(int depth, int breadth, int complexity) {
  }
}

