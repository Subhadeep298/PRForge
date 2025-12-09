<img width="1919" height="946" alt="image" src="https://github.com/user-attachments/assets/0d843e12-8e75-4caa-8eae-e8cd4bb8a002" />
# PRForge

A full-stack application for streamlining Pull Requests, featuring GitHub OAuth integration and Jira ticket visualization.

## Prerequisites
- **Java 17** or higher
- **Node.js** (v18+ recommended) & **npm**
- **Docker** & **Docker Compose** (for the database)

## Getting Started

Follow these instructions to run the project locally.

### 1. Database Setup
Start the PostgreSQL database using Docker Compose:

```bash
docker-compose up -d
```
This will start a Postgres container on port `5432` with the credentials defined in `docker-compose.yml`.

### 2. Backend Setup (Spring Boot)
1. Navigate to the backend directory:
   ```bash
   cd be
   ```

2. Configure Environment Variables:
   Create a `.env` file in the `be` directory.

   > **Tip**: If you received a `.env` file from the project maintainer, simply place it in the `be/` directory.

   If you are setting this up from scratch, create the file with the following content:
   ```env
   GITHUB_CLIENT_ID=your_github_client_id
   GITHUB_CLIENT_SECRET=your_github_client_secret

3. Run the Backend:
   ```bash
   # On Windows
   ./mvnw spring-boot:run

   # On macOS/Linux
   ./mvnw spring-boot:run
   ```
   The backend will start on `http://localhost:8080`.

### 3. Frontend Setup (React + Vite)
1. Navigate to the frontend directory:
   ```bash
   cd ../fe
   ```

2. Install Dependencies:
   ```bash
   npm install
   ```

3. Start the Development Server:
   ```bash
   npm run dev
   ```
   The frontend will typically start on `http://localhost:5173`.

## Usage
1. Open your browser and go to the frontend URL (e.g., `http://localhost:5173`).
2. Click "Get Started" to log in with GitHub.



Category 1: Backend - Java & Spring Boot
1. Q: Why did you choose Spring Boot for the backend over Node.js or Python?

A: I chose Spring Boot for its robustness, strict type safety with Java, and the mature ecosystem. We needed a secure, enterprise-grade framework for handling OAuth2 authentication and structured data (User entities, PR history), which Spring Security and Spring Data JPA handle excellently out of the box.
2. Q: Can you explain how Dependency Injection (DI) works in your project?

A: In PRForge, we use Spring's Inversion of Control (IoC) container. For example, the 
LLMService
 is annotated with @Service, making it a bean. Spring automatically injects this singleton instance into our Controllers via constructor injection, making the code testable and loosely coupled.
3. Q: I see you are using WebClient in 
LLMService
. Why not RestTemplate?

A: RestTemplate is in maintenance mode. WebClient is non-blocking and reactive. Since calling external LLM APIs (like Groq) can have variable latency, using WebClient allows our application to handle these IO-heavy tasks more efficiently without blocking threads, even though our main flow is currently synchronous.
4. Q: How does Spring Security handle the OAuth2 login flow in your app?

A: We use spring-boot-starter-oauth2-client. When a user clicks "Login", they are redirected to the provider (GitHub/Google). Upon successful auth, the provider calls back our backend. We configured a OAuth2LoginSuccessHandler to create a session (JSESSIONID) and persist/update the user in our database before redirecting them to the frontend.
5. Q: What is the purpose of the @Configuration annotation in 
SecurityConfig
?

A: It tells Spring that this class declares one or more @Bean methods (like 
securityFilterChain
) that should be processed by the Spring container to generate bean definitions and service requests for those beans at runtime.
6. Q: How do you handle CORS issues between your Vite frontend and Spring backend?

A: In 
SecurityConfig
, I defined a CorsConfigurationSource bean. I explicitly allowed the frontend origin (http://localhost:5173), enabled credentials (cookies), and permitted standard methods (GET, POST, etc.) so the browser allows the cross-origin requests.
7. Q: You have spring-boot-devtools in your POM. What does it do?

A: It improves the development experience by providing features like automatic restart when files on the classpath change (hot swapping) and disabling template caches, which speeds up the feedback loop during coding.
8. Q: Why use Lombok in your project?

A: Lombok reduces boilerplate code. By annotating my DTOs and Entities with @Data, @Builder, or @RequiredArgsConstructor, I avoid writing getter/setter methods, toString, equals, and constructors manually, keeping the codebase clean.
9. Q: Explain the difference between application.properties and application.yml.

A: They serve the same purpose but have different syntax. properties is flat key-value pairs, while 
yml
 avoids repetition through hierarchy. I prefer YML for readability, especially with nested configurations like Spring Security or Datasource properties.
10. Q: How does Spring Data JPA simplify database access?

A: instead of writing raw SQL or boilerplate JDBC code, I extend JpaRepository. This gives me built-in methods like save(), findById(), and findAll(). For custom queries, I can often just define a method name like findByEmail() and Spring generates the query for me.
11. Q: What is the @Value annotation used for in 
LLMService
?

A: It injects values from the configuration files (like application.properties or environment variables) into fields. I use it to securely inject the ${groq.api-key} so it's not hardcoded in the source code.
12. Q: What is Bean Scoping? What is the default scope?

A: Bean scope defines the lifecycle and visibility of a bean. The default is Singleton (one instance per container). Other scopes include Prototype (new instance every request), Request, and Session (web-aware scopes).
13. Q: How do you handle exceptions in your Spring Boot application?

A: We can use @ControllerAdvice and @ExceptionHandler for global error handling. Locally in 
LLMService
, try-catch blocks are used to catch API errors (like 429 Rate Limit) and return fallback/default responses instead of crashing the request.
14. Q: What is the purpose of spring-dotenv dependency?

A: It allows the application to load environment variables from a .env file during development, mimicking the production environment layout where these secrets would be injected by the platform/CI.
Category 2: Frontend - React & Vite
15. Q: Why use Vite instead of Create React App (CRA)?

A: Vite is significantly faster. It uses native ES modules in the browser during development (no bundling required) and uses Rollup for production builds. High-startup speed and Hot Module Replacement (HMR) make development much smoother than Webpack-based CRA.
16. Q: You are using React 19. What are some new features you are aware of?

A: React 19 introduces the compiler (React Compiler) to auto-memoize components (reducing need for useMemo/useCallback), and new Hooks like useActionState for handling form actions and optimistic updates more natively.
17. Q: Explain the useEffect hook.

A: useEffect handles side, such as data fetching or subscriptions. It takes a dependency array; if the array is empty [], it runs once on mount. If it includes variables, it re-runs when those change. It essentially replaces componentDidMount, componentDidUpdate, and componentWillUnmount.
18. Q: How does the frontend communicate with the backend?

A: We use Axios. We configured an Axios instance (likely with withCredentials: true) to ensure that the JSESSIONID cookie set by the backend during login is sent with every subsequent request for authentication.
19. Q: Why use Tailwind CSS? doesn't it clutter the HTML?

A: While it adds classes to markup, it speeds up development by removing the context switch between CSS files and JS files. It enforces consistency via a design system (utility-first) and results in smaller CSS bundles in production since it purges unused styles.
20. Q: What is the Virtual DOM?

A: It's a lightweight copy of the real DOM. When state changes, React updates the Virtual DOM, compares it with the previous version (diffing), and then only updates the necessary nodes in the real DOM (reconciliation), which is much faster than re-rendering the entire page.
21. Q: How do you manage global state in this app?

A: For a project of this size, we likely use React Context or simply lift state up to common parents (like App.tsx) and pass it down via props. Authentication state (user info) is often kept in a Context to be accessible by all protected routes.
22. Q: What is react-router-dom used for?

A: It handles client-side routing. It allows us to change the URL (e.g., from / to /dashboard) and render different components without triggering a full page reload from the server, making the app feel like a native desktop application.
23. Q: How do you protect routes (e.g., Dashboard) from unauthenticated users?

A: We create a ProtectedRoute wrapper component. It checks if the user object exists (or if the auth cookie is valid). If not, it redirects the user to the /login page using the <Navigate /> component.
24. Q: Explain the concept of "Props Drilling".

A: It's passing data through multiple layers of components that don't need the data themselves, just to reach a deep child component. We avoid this by using Composition (passing components as children) or React Context.
25. Q: What is TypeScript adding to your React project?

A: It adds static typing. It catches errors at compile-time (like passing a string where a number is expected in a component prop) and provides excellent IntelliSense in VS Code, which acts as self-documentation for our component interfaces.
Category 3: Generative AI & Integration
26. Q: How are you generating the PR descriptions?

A: We use the Groq API (hosting Llama 3 models) via our 
LLMService
. We construct a prompt containing the git diff (files changed, additions, deletions) and strictly instruct the model to output the result in a specific format (Title/Description sections).
27. Q: The 
LLMService
 explicitly truncates code at 3000 characters. Why?

A: LLMs have context window limits (token limits). Sending a massive diff for a huge PR would crash the API call or become very expensive. We truncate to ensure we stay within the limit (approx 1000 tokens for output + input) and to reduce latency.
28. Q: How do you handle "Hallucinations" where the AI invents code changes?

A: We reduce temperature (set to 0.7 or lower implies more deterministic). We also strictly ground the prompt: "Focus on WHAT changed, not HOW". Furthermore, we present the result as a "Suggestion" to the user, allowing them to edit it before saving, keeping the human in the loop.
29. Q: Why did you chose Groq/Llama over OpenAI GPT-4?

A: Groq provides extremely fast inference speeds (LPUs) compared to standard GPUs. For a real-time tool like a PR helper, waiting 10 seconds for a description is bad UX. Groq returns it almost instantly. Also, Llama 3 is open-weight and very cost-effective.
30. Q: What is "Prompt Engineering" and how did you apply it here?

A: It's the art of crafting inputs to guide the model. In 
LLMService
, I used techniques like Role Playing ("You are an expert software engineer..."), Few-Shot Prompting (giving an "Example Format"), and Constraint Setting ("50-72 characters", "No bold in bullets") to ensure consistent output.
31. Q: How do you parse the AI response? isn't natural language unpredictable?

A: It is. That's why I instructed the model to use specific markers: TITLE: and DESCRIPTION:. My 
parseResponse
 method looks for these prefixes to extract the data programmatically. If parsing fails, I have a fallback that returns the raw text.
32. Q: What happens if the AI API is down or Rate Limited (429)?

A: I implemented a try-catch block in 
LLMService
. If an exception occurs (specifically checking for "429" or "401"), I return a fallback 
PRSuggestion
 object with a basic template describing the number of files changed, so the user functionality isn't completely broken.
33. Q: What are Tokens?

A: Tokens are the basic units of text for LLMs. Note that 1 token $\approx$ 0.75 words. Prices and limits are calculated in tokens, not characters. My truncation logic currently uses characters (3000), which is a safe approximation for about ~750-1000 tokens.
Category 4: System Design & General
34. Q: How would you scale this if 10,000 users generated PRs simultaneously?

A:
Backend: Horizontal scaling (add more Spring Boot instances) behind a Load Balancer.
Database: Use connection pooling (already in HikariCP) and potentially read replicas.
AI Service: We would hit rate limits. We'd need to implement a Queue (RabbitMQ/Kafka) to decouple the request from the processing, returning a "Job ID" to the frontend to poll for completion.
35. Q: How do you ensure user data security?

A:
We don't store passwords (OAuth delegates this to Google/GitHub).
We use HTTPS (in production).
We sanitize inputs.
Environment variables handle API keys so they aren't checked into version control.
36. Q: Explain the MVC architecture in your backend.

A:
Model: The JPA Entities (User, CompareResult).
View: The JSON responses sent to the React frontend.
Controller: The REST endpoints (@RestController) that receive requests.
Service Layer: Business logic (AI generation, Diff processing) sits here, keeping controllers thin.
37. Q: Why use a Relational Database (PostgreSQL) instead of MongoDB?

A: Our data is structured and relational. Users have PRs, PRs belong to Repositories. Relational DBs enforce data integrity (ACID properties) and schema validation, which is better for user management systems than NoSQL's flexible schema.
38. Q: How does the application handle Git Diff data?

A: The frontend or a backend service fetches the diff from the GitHub API. This raw string (lines starting with + and -) is parsed into a CompareResult object, separating added code from deleted code, which is then fed into the prompt builder.
39. Q: What is "State" vs "Props" in React?

A: State is internal data managed by the component (mutable). Props are data passed down from a parent (immutable/read-only). To change props, a child must communicate back to the parent (usually via a callback function prop).
40. Q: Use of package-lock.json or yarn.lock?

A: These lockfiles ensure deterministic installs. They record the exact version of every dependency installed. This ensures that "works on my machine" also means "works on the production server" because we are using the exact same library versions.
Category 5: Behavioral & Project Specific
41. Q: What was the hardest bug you solved in this project?

A: Sample: "Configuring the OAuth2 redirect was tricky. Spring Security defaults to blocking CORS, and since my frontend runs on port 5173 and backend on 8080, the cookie wasn't being set correctly. I had to explicitly configure the CorsConfigurationSource and set credentials: include on both the client and server."
42. Q: If you had more time, what would you add?

A: I would add:
Result caching (Redis) so identical diffs don't cost API credits.
Custom tone selection for PRs (e.g., "Documentation", "Bugfix", "Feature").
Integration with Jira to auto-link tickets in the description.
43. Q: How did you debug the Groq API integration?

A: I used the logger (LoggerFactory) in 
LLMService
 to print the prompt being built and the raw response. This helped me identify that the JSON response structure was nested (choices[0].message.content), requiring specific traversal logic with Jackson.
44. Q: Why did you structure the folders as be and fe?

A: It's a Monorepo structure. It keeps the distinct concerns separated but co-located for easier full-stack development. It simplifies git operations (one repo to push) but allows independent build pipelines for backend and frontend.
45. Q: How does the 
escapeJson
 method in 
LLMService
 work?

A: When constructing a raw JSON string for the API body, special characters like newlines (\n) or quotes (") inside the user's code diff would break the JSON format. The method manually escapes these characters to ensure validity. (Note: Using ObjectMapper to write the body would be safer/standard, but manual string formatting is faster for simple structures).
46. Q: What validation do you have on the Backend?

A: We use strict typing in Java. Additionally, the 
SecurityConfig
 ensures that unauthenticated users cannot hit the generation endpoints (anyRequest().authenticated()). We also handle null checks in the service layer (e.g., checking if compareResult fields are not null).
47. Q: Explain the 
pom.xml
 dependency management.

A: The dependencies section lists the libraries we need. The plugins section configures how the project is built (e.g., spring-boot-maven-plugin packages the jar as an executable). The parent defines the Spring Boot version and default configurations.
48. Q: How do you handle environment variables locally vs production?

A: Locally we use .env files loaded by spring-dotenv. In production (e.g., Docker/Cloud), we inject them as system environment variables. Spring Boot's property resolution order handles this seamlessly, preferring system vars over file properties.
49. Q: Tell me about the SecurityFilterChain.

A: It's the standard Spring Security mechanism. It's a chain of filters that every request passes through. We customized it to: disable CSRF (common for REST APIs with non-browser clients or stateless auth), setup CORS, define public vs private routes, and attach the OAuth2 success handler.
50. Q: What is the benefit of spring-boot-starter-webflux alongside webmvc?

A: I included it specifically to get WebClient. While the rest of my app uses standard MVC (blocking, Thread-per-request), WebClient provides a modern, fluent API for making HTTP requests to the AI providers, replacing the older RestTemplate.
make me 50 questions and answers for a interview regarding this project

___________________________________________________________________________________________________________________________

Category 1: Jira Integration & API Implementation
1. Q: I see you support both Manual and OAuth connections for Jira. How does the implementation differ?

A: For manual connections, we store the username and apiToken directly and use Basic Auth headers. For OAuth, we store the accessToken and use Bearer Auth headers. The service has separate methods (e.g., 
createAuthHeader
 vs 
createOauthAuthHeader
) to handle these differences while reusing the core parsing logic.
2. Q: Explain how you validate if a Jira connection is correct before saving it.

A: In 
saveJiraConnection
, I call 
testJiraConnection
. This sends a GET request to the Jira Project API (/rest/api/3/project/{key}). I check if the response returns 200 OK and if the returned project name matches the user's input. This "fail-fast" approach prevents saving invalid credentials.
3. Q: What is JQL and how are you using it?

A: JQL (Jira Query Language) is used to filter issues. In 
getAllTicketsByConnectionId
, I construct a JQL string: project={key} ORDER BY created DESC. This allows me to fetch only relevant tickets for the specific project the user connected, rather than fetching all Jira tickets.
4. Q: How do you handle the Atlassian Document Format (ADF) description field?

A: Jira Cloud returns descriptions as a recursive JSON structure (ADF) rather than plain text. I implemented a recursive method 
parseADF(JsonNode node)
 that traverses the content array. It extracts text nodes and handles formatting types like paragraph or hardBreak to reconstruct a readable string.
5. Q: Why do you have a specific check for "Acceptance Criteria" in 
extractIssueDetails
?

A: Many teams define "Done" based on acceptance criteria. I parse the description string line-by-line looking for the phrase "Acceptance Criteria:". Any lines following markers are captured into a list. This allows the AI generation feature to be more accurate by knowing exactly what requirements strictly need to be met.
6. Q: Why did you use RestTemplate instead of WebClient for the Jira Service, when you used WebClient for the LLM Service?

A: (Honest answer) RestTemplate is simple and sufficient for synchronous operations where we don't need high-throughput reactive streams. Since the Jira fetch is triggered by a user action and we wait for the result to display the list, the blocking nature of RestTemplate was acceptable and easier to implement quickly.
7. Q: How do you handle pagination when fetching Jira tickets?

A: Currently, I set maxResults=100 and startAt=0 in the API call variables. For the MVP, this covers most recent use cases. To scale, I would need to check the total field in the response and implement a loop or "Load More" feature to increment startAt.
8. Q: What happens if the Jira API changes its response structure?

A: Since I'm using JsonNode (Jackson's Tree Model) instead of mapping to rigid Java classes, the code is somewhat resilient to extra fields being added. However, if core fields like issues or fields are renamed, the traversal logic would return null/empty. I defend against this with null checks (e.g., if (fields == null) return null;) to prevent crashes.
9. Q: Why do you encode the Basic Auth credentials to Base64 manually?

A: Jira requires the Authorization header format: Basic <base64(email:token)>. I use Base64.getEncoder().encodeToString() to standard compliance. While some HTTP clients do this automatically, doing it manually ensures I have full control over the exact string format being sent.
10. Q: The JiraIssueDetailsResponse uses the Builder pattern. Why?

A: It allows for clean, readable object creation, especially when we have optional fields or need to return different states (success vs failure). It lets me return a response with just a message and success: false without needing to pass null to a constructor for the details field.
Category 2: Spring Boot & Backend Architecture
11. Q: Why storing token and oauthAccessToken with @Column(length = 4096)?

A: Standard database columns (VARCHAR) often default to 255 characters. OAuth tokens and JWTs can easily exceed this length. Explicitly setting length = 4096 ensures the database schema is created wide enough to store these long strings without truncation errors.
12. Q: What is the purpose of @RequiredArgsConstructor in your Service?

A: It's a Lombok annotation that generates a constructor for all final fields. This is the preferred way to do dependency injection in Spring Boot (Constructor Injection). It cleaner than @Autowired on fields and ensures dependencies are not null when the bean is created.
13. Q: How does the JiraConnectionRepo work without an implementation class?

A: It extends JpaRepository. Spring Data JPA creates a proxy implementation at runtime. Method names like findByUserIdAndName are parsed by Spring to automatically generate the SQL query (SELECT * FROM ... WHERE user_id = ? AND name = ?).
14. Q: Why do you return a ValidateResponse object instead of just throwing exceptions?

A: Exceptions are expensive (stack trace generation) and should be reserved for exceptional errors. For business logic validation (e.g., "User already has a connection with this name"), returning a structured response allows the frontend to display a friendly error message without the overhead of HTTP 500 handling.
15. Q: You have @Slf4j on the class. How does logging help here?

A: In integration services, debugging is hard because the error might be on the 3rd party side. I log the flow: "Attempting to save...", "Connection test result...", "Extracted X tickets". If a user says "my tickets aren't showing", I can check the logs to see if we successfully authenticated or if the extraction logic returned 0 items.
16. Q: Explain the @Temporal(TemporalType.TIMESTAMP) annotation.

A: It tells Hibernate how to map the Java Date object to the SQL database type. TIMESTAMP preserves both date and time. This is crucial for oauthAccessTokenExpiresAt so we know exactly when to refresh the token down to the second.
17. Q: Why use UUID for the primary key instead of Long (Auto Increment)?

A: UUIDs are globally unique and safer for exposure in URLs (ticket fetching endpoints rely on connectionId). If I used simple integers (1, 2, 3), a malicious user could guess other users' connection IDs (/tickets/4) easily (Enumeration Attack).
18. Q: In 
getAllTicketsForUserUsingOAuth
, you use streams to filter connections. Why not do it in the Database?

A: Currently, I find the first connection with a non-null token in Java: .filter(c -> c.getOauthAccessToken() != null). Ideally, this should be a database query for performance (findByUserIdAndOauthAccessTokenIsNotNull). I did this for quick prototyping, but moving it to the repository would be an optimization.
19. Q: How does Transactional behavior apply here? (Even if not explicitly annotated)

A: By default, Spring Data JPA repository methods are transactional. However, my service method 
saveJiraConnection
 performs network calls (checking Jira). Ideally, we should avoid keeping a database transaction open during network calls. The 
save
 call happens only after the validation, which is good practice.
20. Q: Why do you have UserDoesNotExists exception?

A: It's a custom runtime exception. It allows the global exception handler to catch this specific case and return a 404 Not Found to the client, distinguishing it from a generic 500 Internal Server Error.
Category 3: Frontend & React (Navbar & Hooks)
21. Q: Explain the useAuth hook usage in 
Navbar.tsx
.

A: useAuth is likely a custom hook accessing a Context context. It abstracts the complexity of user state. Instead of 
Navbar
 needing to know how to check if a user is logged in, it just asks const { user } = useAuth(). This keeps the UI component clean and focused on rendering.
22. Q: What is the backdrop-filter utility doing in your Navbar class?

A: The class backdrop-blur supports-[backdrop-filter]:bg-background/60 creates the "frosted glass" effect. It blurs the content scrolling behind the navbar. The supports- syntax is a Tailwind variant that checks for browser support, falling back to a solid background if the browser doesn't support the blur effect.
23. Q: You are using Radix UI components (DropdownMenu). Why not write your own?

A: Building accessible dropdowns (handling keyboard navigation, screen readers, focus management, closing on outside click) is very hard. Radix provides the "headless" logic for accessibility, while I use Tailwind to style it continuously with my app's theme.
24. Q: What does asChild prop do on DropdownMenuTrigger?

A: It merges the trigger behavior onto the direct child element (my Avatar Button) instead of wrapping it in an extra <span> or <button>. This keeps the DOM clean and prevents layout issues caused by extra wrapper nodes.
25. Q: How do you handle the "Loading" state in the Navbar?

A: I use conditional rendering. loading ? (...) : user ? (...) : (...). While checking auth status, I display a "skeleton" loader (h-9 w-24 animate-pulse bg-muted). This prevents the "flash of unauthenticated content" where the Login button might flicker briefly before the User avatar appears.
26. Q: Why use AvatarFallback?

A: If the user's avatar_url (from GitHub) fails to load (broken link) or hasn't loaded yet, AvatarFallback shows. I display the first initial (user.name?.charAt(0)). This ensures the UI never looks broken or empty.
27. Q: Explain the logic behind user.jira_connected && (...) in the dropdown.

A: This is conditional rendering. The specific menu item "Disconnect Jira" only exists in the DOM if the user object has the jira_connected flag set to true. This improves UX by hiding irrelevant actions from users who haven't connected Jira yet.
28. Q: Why use onClick={logout} on a MenuItem instead of a generic <a href="/logout">?

A: Using href would cause a full page refresh. logout is likely a function in my AuthContext that clears the Client-side state (user = null) and might make an async API call to the backend to invalidate the session cookie, providing a smoother transition.
29. Q: What are the bg-background/95 and border-border/40 classes?

A: These use Tailwind's opacity modifier syntax. bg-background uses the CSS variable for the theme's background color, and /95 sets it to 95% opacity. This aligns with modern design trends where UI elements are slightly translucent.
30. Q: How does the Navbar ensure it stays on top of other content?

A: Although not explicitly seen in the snippet, standard Navbars usually use sticky top-0 z-50. The z-index ensures it layers above scrollable content like the PR code blocks or maps.
Category 4: Security & Best Practices
31. Q: Why is the 
User
 entity injected into 
JiraConnectionServiceImpl
?

A: To perform validation. Before saving a connection, we check userService.validateUserByProviderId. This creates a foreign-key-like integrity check at the application level, ensuring we don't create orphan connections for users that don't exist.
32. Q: How do you secure the Jira Tokens in the database?

A: Currently, they are stored as plain text strings (String token). Self-Correction: In a real production environment, this is a security risk. I should encrypt these tokens using @Convert with a custom JPA AttributeConverter (using AES encryption) so that even if the DB is dumped, the tokens are unreadable.
33. Q: What is the risk of logging e.getMessage() in the catch blocks?

A: It's generally safe, but one must be careful. If the RestTemplate error message contains the full request URL with the API Key (sometimes passed in query params), I might accidentally log secrets. However, since I send tokens in Headers, the error message from Jira usually just says "401 Unauthorized" which is safe to log.
34. Q: Why is csrf disabled in 
SecurityConfig
 (from previous context)?

A: Since we generally use stateless authentication patterns or rely on SameSite cookie policies for modern SPA (Single Page Application) interactions, strict CSRF tokens can complicate the setup. For an MVP, disabling it is a trade-off for development speed, but for production, we should enable it or rely on SameSite=Strict cookies.
35. Q: How does the application distinguish between Manual and OAuth connections?

A: The 
JiraConnection
 entity has fields for both. If oauthAccessToken is not null, the logic assumes it's an OAuth connection. If token (api token) is not null, it's manual. The service logic prioritizes looking for one or the other based on the method called.
36. Q: What happens if the Jira Access Token expires?

A: Currently, the 
getTicketByIdUsingOAuth
 method just tries to use the token. If it fails (401), it throws an error. Ideally, I need to check oauthAccessTokenExpiresAt before the call. If it's expired, I should use the oauthRefreshToken to request a new access token from Jira before attempting the API call.
37. Q: Why do you separate JiraConnectionRequestDto from the Entity?

A: Separation of concerns. The DTO defines exactly what the API expects from the frontend (inputs). The Entity defines how data is stored in the DB (internal structure). This allows me to change the DB schema without breaking the public API, and vice-versa.
Category 5: Problem Solving & Edge Cases
38. Q: How would you debug "Connection test failed" if the logs just say "false"?

A: I would temporarily increase log verbosity or inspect the catch block in 
doesConnectionExistandNameMatch
. I might print the response.getStatusCode() and response.getBody() to see if Jira returned a 403 (Forbidden permissions) or 404 (Project key doesn't exist).
39. Q: A user claims they connected Jira but cannot see their project. Why?

A: Jira API Tokens are per-user. If the user created a token but their Jira account doesn't have permission to view that specific project, the API call will fail even if the token is valid. Access is determined by the Jira permissions scheme.
40. Q: Why does 
extractIssueDetails
 return null acceptance criteria if the format is wrong?

A: The code looks specifically for "Acceptance Criteria:". If the user typed "AC:" or "Requirements:", it would be missed. To improve this, I could use a regex or a list of synonyms to make the parser more robust to different writing styles.
41. Q: How do you handle multiple connections for one user?

A: The database findAllByUserId returns a List. The method 
getAllConnectionsByUserid
 maps this list to a Map of Name -> ID. The frontend likely displays a dropdown allowing the user to switch between different Jira configurations (e.g., Work vs Personal Jira).
42. Q: What happens if two tickets have the same Key but different Projects?

A: Connections are scoped by connectionId. When fetching a ticket, we first retrieve the specific Connection context. This means even if PROJ-1 exists in two different Jira instances connected by the user, the connectionId ensures we query the correct domain url and credentials.
43. Q: How do you protect against "Slowloris" or slow Jira API responses?

A: Currently, RestTemplate is blocking. If Jira hangs, my thread hangs. To fix this, I should configure a ConnectTimeout and ReadTimeout on the RestTemplate bean definition (e.g., 5 seconds). If Jira is too slow, we abort and clean up the thread.
44. Q: Why UUID.randomUUID() for ID generation manually in the Service?

A: Usually, we let the DB handle generation (@GeneratedValue). Doing it manually in the service allows us to return the ID immediately or use it in subsequent logic (like logging) before the transaction commits or the entity is flushed to the DB.
45. Q: What if the domainUrl entered ends with a slash (/)?

A: My code constructs URLs like domainUrl + "/rest/...". If the user inputs https://jira.com/, the result is https://jira.com//rest/.... While most servers handle double slashes, it's safer to trim trailing slashes in the DTO or Service to ensure clean URL construction.
46. Q: Why use equalsIgnoreCase for project name validation?

A: User inputs are often case-insensitive mentally, but APIs can be strict. "PRFORGE" vs "PrForge". Comparing correctly ensures we don't annoy the user with false negatives just because of capitalization.
47. Q: In 
Navbar.tsx
, how does forceMount on DropdownContent help?

A: It ensures the content is rendered in the React tree (though hidden) or helps with animation libraries ensuring the entry animation starts from a mounted state. It's often required when using complex unmount animations with Radix UI.
48. Q: How scalable is the 
extractTicketKeys
 map approach?

A: It stores Id -> Key. For 100 tickets, it's trivial. If we fetched 10,000 tickets, this Map would grow. However, since we paginate to 100 results (maxResults=100), memory consumption within this request is strictly bounded and safe.
49. Q: Why did you implement Disconnect Jira in the Navbar?

A: It gives users control. If they authorized the wrong account or want to revoke access, they need an easy UI way to clear the backend state (delete the row from jira_connection table) without needing support intervention.
50. Q: Overall, what is your approach to testing this integration?

A:
Unit Tests: Mock the RestTemplate to return sample JSON responses and verify that 
extractIssueDetails
 parses the ADT correctly.
Integration Tests: Use @SpringBootTest with an H2 database to verify the Repository saves correctly.
Manual: Connect to a real free-tier Jira Cloud instance to verify the actual API contract hasn't drifted.
