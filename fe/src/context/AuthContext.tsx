import React, { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import axios from 'axios';

interface User {
    id: string;
    name: string;
    avatar_url: string;
    email: string;
    jira_connected?: boolean;
}

interface AuthContextType {
    user: User | null;
    loading: boolean;
    login: () => void;
    logout: () => Promise<void>;
    disconnectJira: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);

    const fetchUser = async () => {
        try {
            const response = await axios.get('http://localhost:8080/api/user', {
                withCredentials: true,
            });
            if (response.data && response.data.id) {
                setUser(response.data);
                document.cookie = `user_id=${response.data.id}; path=/; max-age=86400; SameSite=Lax`;
            } else {
                setUser(null);
            }
        } catch (error) {
            setUser(null);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchUser();
    }, []);

    const login = () => {
        window.location.href = 'http://localhost:8080/oauth2/authorization/github';
    };

    const logout = async () => {
        try {
            // If connected to Jira, disconnect first as requested
            if (user?.jira_connected) {
                try {
                    await axios.delete('http://localhost:8080/api/jira/disconnect', { withCredentials: true });
                } catch (jiraError) {
                    console.error("Failed to disconnect Jira during logout", jiraError);
                    // Continue with logout even if disconnect fails
                }
            }

            await axios.post('http://localhost:8080/logout', {}, {
                withCredentials: true,
            });
            setUser(null);
            document.cookie = "user_id=; path=/; max-age=0; SameSite=Lax";
            window.location.href = '/';
        } catch (error) {
            console.error('Logout failed', error);
        }
    };

    const disconnectJira = async () => {
        try {
            await axios.delete('http://localhost:8080/api/jira/disconnect', { withCredentials: true });
            // Redirect to GitHub login to restore GitHub identity/session
            window.location.href = 'http://localhost:8080/oauth2/authorization/github';
        } catch (e) {
            console.error("Failed to disconnect Jira", e);
        }
    };

    return (
        <AuthContext.Provider value={{ user, loading, login, logout, disconnectJira }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};


