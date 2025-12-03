import React from 'react';
import { Button } from "@/components/ui/button";


const Home: React.FC = () => {
    const handleLogin = () => {
        window.location.href = 'http://localhost:8080/oauth2/authorization/github';
    };

    return (
        <div className="flex min-h-[calc(100vh-4rem)] flex-col items-center justify-center py-12 md:py-24 lg:py-32 bg-background">
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
                        <Button onClick={handleLogin} size="lg" className="h-12 px-8">
                            Get Started
                        </Button>
                        <Button variant="outline" size="lg" className="h-12 px-8">
                            About Us
                        </Button>
                    </div>
                </div>


            </div>
        </div>
    );
};

export default Home;
