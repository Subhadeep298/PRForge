import React, { useEffect, useState } from "react";
import axios from "axios";
import { Button } from "@/components/ui/button";

const JiraConnect: React.FC = () => {
  const [jiraConnected, setJiraConnected] = useState(false);
  const [loading, setLoading] = useState(false);

  // If you still want to show “connected” based on backend data,
  // expose an endpoint like GET /jiraConnection/oauth/allTickets or /jiraConnection/oauth/status
  const checkJiraConnection = async () => {
    try {
      setLoading(true);
      const response = await axios.get(
        "http://localhost:8080/jiraConnection/oauth/allTickets",
        {
          withCredentials: true,
        }
      );
      // If API returns a map of tickets, consider “connected” if no error
      setJiraConnected(true);
    } catch (err) {
      setJiraConnected(false);
    } finally {
      setLoading(false);
    }
  };

  const handleJiraConnect = () => {
    // Let Spring Security redirect to Atlassian using the jira registration
    // This matches spring.security.oauth2.client.registration.jira
    window.location.href = "http://localhost:8080/oauth2/authorization/jira";
  };

  useEffect(() => {
    checkJiraConnection();
  }, []);

  return (
    <div className="p-4">
      {loading ? (
        <p>Loading...</p>
      ) : jiraConnected ? (
        <p className="text-green-600 font-semibold">Jira Connected ✅</p>
      ) : (
        <Button onClick={handleJiraConnect} className="gap-2 font-semibold">
          Connect Jira
        </Button>
      )}
    </div>
  );
};

export default JiraConnect;
