import React, { useRef } from 'react';
import { Button } from "@/components/ui/button";
import { useAuth } from '../context/AuthContext';
import CompareForm from '@/components/CompareForm';

const Home: React.FC = () => {
    const { user, login } = useAuth();
    const formRef = useRef<HTMLDivElement>(null);

    const handleJiraConnect = () => {
        window.location.href = 'http://localhost:8080/oauth2/authorization/jira';
    };

    const handleGetStarted = () => {
        if (user) {
            // User is logged in, scroll to form
            formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        } else {
            // User is not logged in, redirect to login
            login();
        }
    };

    return (
        <div className="flex min-h-screen flex-col bg-background">
            {/* Hero Section */}
            <div className="flex min-h-[calc(100vh-4rem)] flex-col items-center justify-center py-12 md:py-24 lg:py-32">
                <div className="container px-4 md:px-6">
                    <div className="flex flex-col items-center space-y-4 text-center">
                        <div className="space-y-4">
                            <h1 className="text-3xl font-bold tracking-tighter sm:text-4xl md:text-5xl lg:text-6xl/none pixel-gradient bg-clip-text text-transparent pb-2">
                                Streamline Your Pull Requests
                            </h1>
                            <p className="mx-auto max-w-[700px] text-gray-400 md:text-xl">
                                Experience a seamless workflow for creating and updating pull requests. Integrate Jira tickets or Github issues, and code change to generate detailed descriptions effortlessly.
                            </p>
                        </div>
                        <div className="space-x-4">
                            {!user ? (
                                <Button onClick={handleGetStarted} size="lg" className="h-12 px-8">
                                    Get Started
                                </Button>
                            ) : !user.jira_connected ? (
                                <>
                                    <Button onClick={handleGetStarted} size="lg" className="h-12 px-8">
                                        Get Started
                                    </Button>
                                    <Button onClick={handleJiraConnect} size="lg" variant="secondary" className="h-12 px-8">
                                        Connect Jira
                                    </Button>
                                </>
                            ) : (
                                <Button onClick={handleGetStarted} size="lg" className="h-12 px-8">
                                    Get Started
                                </Button>
                            )}

                            <Button variant="outline" size="lg" className="h-12 px-8">
                                About Us
                            </Button>
                        </div>
                    </div>
                </div>
            </div>

            {/* Compare Form Section */}
            {user && (
                <div ref={formRef} className="flex min-h-screen flex-col items-center justify-center py-12 md:py-24 lg:py-32 px-4">
                    <CompareForm />
                </div>
            )}
        </div>
    );
};

export default Home;
